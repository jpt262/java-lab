package com.weworklab.searcher2;

/**
 * Created by jtres on 2/20/18.
 */
public class Site {
    private final int index;
    private int rank;
    private String url;
    private int linkingRootDomains;
    private int externalLinks;
    private float mozRank;
    private float mozTrust;

    public Site(int rank, String url, int linkingRootDomains, int externalLinks, float mozRank, float mozTrust) {
        this.index = rank - 1;
        this.rank = rank;
        this.url = url;
        this.linkingRootDomains = linkingRootDomains;
        this.externalLinks = externalLinks;
        this.mozRank = mozRank;
        this.mozTrust = mozTrust;
    }

    public Integer getIndex () {
        return this.index;
    }

    public Integer getRank () {
        return this.rank;
    }

    public String getUrl () {
        return this.url;
    }
}
