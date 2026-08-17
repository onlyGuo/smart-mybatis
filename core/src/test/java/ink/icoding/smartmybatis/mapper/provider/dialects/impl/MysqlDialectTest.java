package ink.icoding.smartmybatis.mapper.provider.dialects.impl;

import ink.icoding.smartmybatis.conf.GlobalConfig;
import ink.icoding.smartmybatis.conf.SmartConfigHolder;
import ink.icoding.smartmybatis.entity.expression.Where;
import ink.icoding.smartmybatis.entity.po.PO;
import junit.framework.TestCase;

public class MysqlDialectTest extends TestCase {

    private final MysqlDialect dialect = new MysqlDialect();

    @Override
    protected void setUp() {
        SmartConfigHolder.init(new GlobalConfig(), dialect);
    }

    public void testBuildWherePartIncludesOrderByWithoutConditionsOrLimit() {
        Where where = Where.where().orderBy(SortableRecord::getSortRank).desc();

        String sql = dialect.buildWherePart(where, false, "");

        assertEquals(" ORDER BY _t.`SORT_RANK` DESC", sql);
    }

    private static class SortableRecord extends PO {
        private Integer sortRank;

        public Integer getSortRank() {
            return sortRank;
        }
    }
}
