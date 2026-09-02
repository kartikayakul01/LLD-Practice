package models;
import java.util.*;
public class URL_Shortner{
    private Map<String,String> Url_Collection = new HashMap<>();
    private final String INTIALPART = "https://ourwebsite/";
    private static Integer ournumber= 999;

    private Boolean URLValidator(String url){
        if(!url.startsWith("https://")){
            return false;
        }
        return  true;
    }

    private String ShortnerUrl(String url){
        ournumber++;
        return INTIALPART+String.valueOf(ournumber);
    }

    public String createUrl(String Orignalurl){
        if(!URLValidator(Orignalurl)){
            throw new IllegalArgumentException("Url is not valid");
        }
        String shorturl;
        do{
            shorturl = ShortnerUrl(Orignalurl);
        }while(Url_Collection.containsKey(shorturl));
        Url_Collection.put(shorturl,Orignalurl);
        return shorturl;
    }
    public String getUrl(String url){
        if(Url_Collection.containsKey(url)){
            return Url_Collection.get(url);
        }else{
            return "DO NOT EXITS";
        }
    }
}