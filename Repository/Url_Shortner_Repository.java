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
            URL_Slave.put(ShortURL,Orignalurl);
        });
        return true;
    }
    public String get_url(String shorturl) throws Exception {
        String fromSlave = URL_Slave.get(shorturl);
        if (fromSlave != null) {
            return fromSlave;
        }
        String fromMaster = URL_Master.get(shorturl);
        if (fromMaster != null) {
            return fromMaster;
        }
        throw new Exception("This url do not exists");
    }
    public Boolean url_available(String shorturl){
        if(!URL_Master.containsKey(shorturl)){
            return true;
        }
        return false;
    }
}