package service;


import Repository.Url_Shortner_Repository;
import Strategy.Url_Shortner_Strategy;

public class Url_Shortner_Service{
    private Url_Shortner_Repository Storage;
    private Url_Shortner_Strategy Shortning_Strategy;
    public Url_Shortner_Service(Url_Shortner_Repository storage_used, Url_Shortner_Strategy strategy){
        Storage= storage_used;
        Shortning_Strategy = strategy;
    }

    private Boolean URLValidator(String url){
        if(!url.startsWith("https://")){
            return false;
        }
        return  true;
    }
    public String save_url(String Orignalurl){
        if(!URLValidator(Orignalurl)){
            throw new IllegalArgumentException("Url is not valid");
        }
        String shorturl;
        do{
            shorturl = Shortning_Strategy.short_url(Orignalurl);
        }while(!Storage.url_available(shorturl));
        Storage.save_url(shorturl,Orignalurl);
        return shorturl;
    }
    public String getUrl(String Shorturl) throws Exception {

        try {
            return Storage.get_url(Shorturl);
        } catch (Exception e) {
            throw new Exception("DO NOT EXITS");
        }

    }
}