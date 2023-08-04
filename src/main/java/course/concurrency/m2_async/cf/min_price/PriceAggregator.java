package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PriceAggregator {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {

        List<CompletableFuture<Double>> futures = getCompletableFutures(itemId);
        CompletableFuture<List<Double>> allResults = getAllResults(futures);
        return allResults.join().stream().min(Double::compareTo).orElse(Double.NaN);
    }

    private static CompletableFuture<List<Double>> getAllResults(List<CompletableFuture<Double>> futures) {
        return CompletableFuture.allOf().thenApply(
                v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );
    }

    private List<CompletableFuture<Double>> getCompletableFutures(long itemId) {
        return shopIds.stream()
                .map(shopId -> CompletableFuture.supplyAsync(
                        () -> priceRetriever.getPrice(itemId, shopId), EXECUTOR_SERVICE)
                        .completeOnTimeout(Double.NaN, 2950, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> Double.NaN))
                .collect(Collectors.toList());
    }


}