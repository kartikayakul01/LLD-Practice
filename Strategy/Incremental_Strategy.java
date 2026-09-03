package Strategy;

import java.util.concurrent.atomic.AtomicInteger;

public class Incremental_Strategy implements Url_Shortner_Strategy{
    private final AtomicInteger ournumber= new AtomicInteger(999);

    private String ShortnerUrl(String url){
        int next = ournumber.incrementAndGet();
        return INTIALPART+String.valueOf(next);
    }
    @Override
    public String short_url(String OrignalUrl) {
        return ShortnerUrl(OrignalUrl);
    }
}