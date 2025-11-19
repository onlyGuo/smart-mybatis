package ink.icoding.smartmybatis.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * 雪花算法 ID 生成器
 * @author gsk
 */
public class SnowflakeIdGeneratorUtil {
    // 起始时间戳（自定义，建议固定不变）
    private final static long START_STAMP = 1609459200000L;

    // 序列号占用位数
    private final static long SEQUENCE_BIT = 12;
    // 机器标识占用位数
    private final static long MACHINE_BIT = 5;
    // 数据中心占用位数
    private final static long DATACENTER_BIT = 5;

    private final static long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTAMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    // 数据中心
    private final long datacenterId;
    // 机器标识
    private final long machineId;
    // 序列号
    private long sequence = 0L;
    // 上一次时间戳
    private long lastStamp = -1L;

    private static SnowflakeIdGeneratorUtil instance;

    public SnowflakeIdGeneratorUtil(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than " + MAX_DATACENTER_NUM + " or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than " + MAX_MACHINE_NUM + " or less than 0");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 获取实例
     * @param datacenterId 数据中心ID
     * @param machineId 机器ID
     * @return 实例
     */
    public static SnowflakeIdGeneratorUtil getInstance(long datacenterId, long machineId) {
        return new SnowflakeIdGeneratorUtil(datacenterId, machineId);
    }

    /**
     * 获取实例（自动生成数据中心ID和机器ID）
     * @return 实例
     */
    public static SnowflakeIdGeneratorUtil getInstance() {
        synchronized (SnowflakeIdGeneratorUtil.class) {
            if (instance == null) {
                long datacenterId = getDatacenterId();
                long machineId = getMachineId(datacenterId);
                instance = new SnowflakeIdGeneratorUtil(datacenterId, machineId);
            }
        }
        return instance;
    }

    // 根据MAC地址生成数据中心ID
    private static long getDatacenterId() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isLoopback() && ni.getHardwareAddress() != null) {
                    byte[] mac = ni.getHardwareAddress();
                    long id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    return id % (SnowflakeIdGeneratorUtil.MAX_DATACENTER_NUM + 1);
                }
            }
        } catch (Exception ignored) {}
        return 1L;
    }

    // 根据IP和数据中心ID生成机器ID
    private static long getMachineId(long datacenterId) {
        StringBuilder sb = new StringBuilder();
        sb.append(datacenterId);
        try {
            InetAddress ip = InetAddress.getLocalHost();
            sb.append(ip.getHostAddress());
        } catch (Exception e) {
            // ignore
        }
        return (sb.toString().hashCode() & 0xfffffff) % (SnowflakeIdGeneratorUtil.MAX_MACHINE_NUM + 1);
    }

    public synchronized long nextId() {
        long currStamp = getNewStamp();
        if (currStamp < lastStamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (currStamp == lastStamp) {
            // 同一毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0L) {
                // 序列号用完，等待下一毫秒
                currStamp = getNextMill();
            }
        } else {
            sequence = 0L; // 新的一毫秒，序列号重置
        }

        lastStamp = currStamp;

        return ((currStamp - START_STAMP) << TIMESTAMP_LEFT)
                | (datacenterId << DATACENTER_LEFT)
                | (machineId << MACHINE_LEFT)
                | sequence;
    }

    private long getNextMill() {
        long mill = getNewStamp();
        while (mill <= lastStamp) {
            mill = getNewStamp();
        }
        return mill;
    }

    private long getNewStamp() {
        return System.currentTimeMillis();
    }

    // 测试
    public static void main(String[] args) {
        long l = SnowflakeIdGeneratorUtil.getInstance().nextId();
        System.out.println(l);
    }
}
