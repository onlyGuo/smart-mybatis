package ink.icoding.smartmybatis.mapper.interfaces;

import ink.icoding.smartmybatis.entity.po.PO;
import ink.icoding.smartmybatis.mapper.base.SmartMapper;

/**
 * Smart Mapper 初始化器接口
 * 用于在 Smart Mapper 创建时进行自定义初始化操作
 * @author gsk
 */
public interface SmartMapperInitializer {

    <T extends PO> void initMapper(SmartMapper<T> smartMapper);

}
