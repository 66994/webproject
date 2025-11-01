package com.digital.service.BossZhiPinCrawler;

// 招聘信息数据类
public class WorkInfService {
    // 招聘链接
    private String url;
    // 工作名
    private String workName;
    // 薪水
    private String workSalary;
    // 工作地址
    private String workAddress;
    // 工作内容
    private String workContent;
    // 要求工作年限
    private String workYear;
    // 学历
    private String graduate;
    // 招聘人什么时候活跃
    private String HRTime;
    // 公司名
    private String companyName;

    // Getter和Setter方法
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWorkName() {
        return workName;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public String getWorkSalary() {
        return workSalary;
    }

    public void setWorkSalary(String workSalary) {
        this.workSalary = workSalary;
    }

    public String getWorkAddress() {
        return workAddress;
    }

    public void setWorkAddress(String workAddress) {
        this.workAddress = workAddress;
    }

    public String getWorkContent() {
        return workContent;
    }

    public void setWorkContent(String workContent) {
        this.workContent = workContent;
    }

    public String getWorkYear() {
        return workYear;
    }

    public void setWorkYear(String workYear) {
        this.workYear = workYear;
    }

    public String getGraduate() {
        return graduate;
    }

    public void setGraduate(String graduate) {
        this.graduate = graduate;
    }

    public String getHRTime() {
        return HRTime;
    }

    public void setHRTime(String HRTime) {
        this.HRTime = HRTime;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return "WorkInf{" +
                "url='" + url + '\'' +
                ", workName='" + workName + '\'' +
                ", workSalary='" + workSalary + '\'' +
                ", workAddress='" + workAddress + '\'' +
                ", workContent='" + workContent + '\'' +
                ", workYear='" + workYear + '\'' +
                ", graduate='" + graduate + '\'' +
                ", HRTime='" + HRTime + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}