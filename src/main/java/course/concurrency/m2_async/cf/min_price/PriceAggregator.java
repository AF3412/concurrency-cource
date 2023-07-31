package course.concurrency.m2_async.cf.min_price;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        executor.setKeepAliveTime(3, TimeUnit.SECONDS);

        CompletableFuture<Double>[] array = new CompletableFuture[shopIds.size()];
        int count = 0;
        for (Long shopId : shopIds) {
            CompletableFuture<Double> future = CompletableFuture.supplyAsync(
                            () -> priceRetriever.getPrice(itemId, shopId),
                            executor)
                                .completeOnTimeout(Double.NaN, 3, TimeUnit.SECONDS)
                                .exceptionally(ex -> Double.NaN);
            array[count++] = future;
        }

        CompletableFuture<List<Double>> allResults = CompletableFuture.allOf(array)
                .thenApplyAsync(v -> Arrays.stream(array)
                        .map(CompletableFuture::join)
                        .peek(System.out::println)
                        .collect(Collectors.toList()));

        return allResults.join().stream().min(Double::compareTo).orElse(Double.NaN);
    }
}
