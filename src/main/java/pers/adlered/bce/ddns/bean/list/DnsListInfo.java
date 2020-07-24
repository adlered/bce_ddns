package pers.adlered.bce.ddns.bean.list;

/**
 * <h3>bce_ddns</h3>
 * <p>Info.</p>
 *
 * @author : https://github.com/adlered
 * @date : 2020-07-24
 **/
public class DnsListInfo {

    public int recordId;
    public String domain;
    public String view;
    public String rdtype;
    public int ttl;
    public String rdata;
    public String zoneName;
    public String status;

    public DnsListInfo(int recordId, String domain, String view, String rdtype, int ttl, String rdata, String zoneName, String status) {
        this.recordId = recordId;
        this.domain = domain;
        this.view = view;
        this.rdtype = rdtype;
        this.ttl = ttl;
        this.rdata = rdata;
        this.zoneName = zoneName;
        this.status = status;
    }
}
