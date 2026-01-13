package ink.icoding.smartmybatis.example.entity;

import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.entity.po.enums.ID;
import ink.icoding.smartmybatis.entity.po.enums.TableField;
import ink.icoding.smartmybatis.entity.po.enums.TableName;

import java.util.List;

/**
 * 中国城市经纬度表 china_cities
 * @author gsk
 */
@TableName(init = "testInitSql.sql")
public class ChinaCities extends PO {

    @ID
    private int id;
    /**
     * 行政区划代码
     */
    private String adcode;
    /**
     * 城市全称
     */
    private String name;
    /**
     * 城市简称
     */
    private String shortName;
    /**
     * 上级(省份)
     */
    private String parentName;

    /**
     * 级别
     */
    private double lng;

    /**
     * 纬度
     */
    private double lat;

    /**
     * 天气预警信息
     */
    @TableField(exist = false)
    private List<Object> weatherAlarms;
}
