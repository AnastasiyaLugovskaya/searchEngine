package searchengine.dto.indexing;

import searchengine.config.Site;
import searchengine.config.SitesList;

public class Utils {

    public static String getRelativeUrl(String url, SitesList sites){
        Site siteToMatch = null;
        for (Site site : sites.getSites()){
            if (url.contains(site.getUrl())){
                siteToMatch = site;
            }
        }
        String root = siteToMatch.getUrl();
        int start = url.indexOf(root) + root.length() - 1;
        int end = url.length();
        return url.substring(start, end);
    }
}
