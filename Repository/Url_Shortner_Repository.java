package Repository;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Url_Shortner_Repository{
    private Map<String,String> URL_Master;
    private Map<String,String> URL_Slave;

    public Url_Shortner_Repository(){
        URL_Master = new ConcurrentHashMap<>();
        URL_Slave = new ConcurrentHashMap<>();
    }

    public Boolean save_url(String ShortURL,String Orignalurl){
        URL_Master.put(ShortURL,Orignalurl);
        CompletableFuture.runAsync(()-> {
            System.out.println("---- Updating slave---");
            URL_Slave.put(ShortURL,Orignalurl);
            System.out.println("---- slave updated---");

        });
        return true;
    }
    public String get_url(String shorturl) throws Exception {
        if(!URL_Slave.containsKey(shorturl)){
            throw new Exception("This url do not exists");
        }
        return URL_Slave.get(shorturl);
    }
    public Boolean url_available(String shorturl){
        if(!URL_Master.containsKey(shorturl)){
            return true;
        }
        return false;
    }
}