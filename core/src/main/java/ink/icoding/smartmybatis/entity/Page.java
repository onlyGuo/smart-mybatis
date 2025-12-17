package ink.icoding.smartmybatis.entity;

/**
 * 分页参数
 * @author gsk
 */
public class Page {

    /**
     * 页码
     */
    private int page;

    /**
     * 每页大小
     */
    private int pageSize;

    /**
     * 构造函数
     * @param page 页码
     * @param pageSize 每页大小
     */
    public Page(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 默认构造函数，默认页码为1，每页大小为10
     */
    public Page (){
        this.page = 1;
        this.pageSize = 10;
    }

    /**
     * 获取页码
     * @return 页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 设置页码
     * @param page 页码
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 获取每页大小
     * @return 每页大小
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置每页大小
     * @param pageSize 每页大小
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

}
