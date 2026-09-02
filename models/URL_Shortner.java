package models;
import java.util.*;
public class URL_Shortner{
    private Map<String,String> Url_Collection = new HashMap<>();
    private final String INTIALPART = "https://ourwebsite/";
    private static Integer ournumber= 1000;

    private Boolean URLValidator(String url){
        if(!url.startsWith("https://")){
            return false;
        }
        return  true;
    }

    private String ShortnerUrl(String url){
        return INTIALPART;
    }

    public String createUrl(String Orignalurl){
        if(!URLValidator(Orignalurl)){
            throw new IllegalArgumentException("Url is not valid");
        }
        return ShortnerUrl(Orignalurl);
    }
}