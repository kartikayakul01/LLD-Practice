package Controller;

import service.Url_Shortner_Service;

public class Url_Shortner_Controller{
    private Url_Shortner_Service service;

    public Url_Shortner_Controller(Url_Shortner_Service service_used){
        service = service_used;
    }

    public String create_url(String OrignalUrl){
        return service.save_url(OrignalUrl);
    }
    public String read_url(String ShortUrl) throws Exception{
        return service.getUrl(ShortUrl);
    }
}