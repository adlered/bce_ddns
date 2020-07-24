package pers.adlered.bce.ddns.bean.list;

import java.util.List;

/**
 * <h3>bce_ddns</h3>
 * <p>List 解析.</p>
 *
 * @author : https://github.com/adlered
 * @date : 2020-07-24
 **/
public class DnsList {

    public String orderBy;
    public String order;
    public int pageNo;
    public int pageSize;
    public List<DnsListInfo> result;
    public int totalCount;

    public DnsList(String orderBy, String order, int pageNo, int pageSize, List<DnsListInfo> result, int totalCount) {
        this.orderBy = orderBy;
        this.order = order;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.result = result;
        this.totalCount = totalCount;
    }
}
