package paradigma.pocs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import paradigma.pocs.dto.Tweet.Tweet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RestController
public class TweetController {

    final private static Integer NUM_REQUEST = 200;

    final private static Logger log = LoggerFactory.getLogger(TweetController.class);

    private static String urlSlowService;

    private static String baseUrlOms;

    private RestTemplate restTemplate;

    public TweetController(@Value("${urlSlowService}") String urlSlowService,
                            @Value("${baseUrlOms}") String baseUrlOms) {
        restTemplate = new RestTemplate();
        this.urlSlowService = urlSlowService;
        this.baseUrlOms = baseUrlOms;
    }

    @GetMapping("/slow-service-tweets")
    private List<Tweet> getAllTweets() throws InterruptedException {
        Thread.sleep(2000L); // delay
        return Arrays.asList(
                new Tweet("RestTemplate rules", "@user1"),
                new Tweet("WebClient is better", "@user2"),
                new Tweet("OK, both are useful", "@user1"));
    }

    @GetMapping("/tweets-non-blocking-future")
    public List<Tweet> getTweetsNonBlockingFuture() throws Throwable {
        log.info("Starting BLOCKING Controller!");


        List<CompletableFuture<List<Tweet>>> futuresList = new ArrayList<>();

        long start = System.currentTimeMillis();
        for (int i=0;i<NUM_REQUEST;i++) {
            CompletableFuture<List<Tweet>> completableFutureCompletableFuture = CompletableFuture.supplyAsync(() -> getTweets(), Executors.newFixedThreadPool(100));
            futuresList.add(completableFutureCompletableFuture);
        }
        log.info("Time to prepare :" + (System.currentTimeMillis() - start) + "ms");

        CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]))
                // avoid throwing an exception in the join() call
                .exceptionally(ex -> null)
                .join();

        List<Tweet> combined = futuresList.stream()
                .map(CompletableFuture::join).flatMap(List::stream)
                .collect(Collectors.toList());

        log.info("Ending BLOCKING Controller!");
        return combined;
    }

    private List<Tweet> getTweets() {
        List<Tweet> response = restTemplate.exchange(
                urlSlowService, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Tweet>>() {
                }).getBody();
        return response;
    }

    @GetMapping(value = "/tweets-non-blocking-webclient")
    public Flux<Tweet> getTweetsNonBlockingWebclient() {
        log.info("Starting NON-BLOCKING Controller!");
        List<Flux<Tweet>> fluxes = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (int i=0;i<NUM_REQUEST;i++) {
            fluxes.add(WebClient.create()
                    .get()
                    .uri(urlSlowService)
                    .retrieve()
                    .bodyToFlux(Tweet.class));
        }

        log.info("Time to prepare :" + (System.currentTimeMillis() - start) + "ms");
        log.info("Exiting NON-BLOCKING Controller!");
        return Flux.merge(fluxes);
    }


    @GetMapping(value = "/stock-oms")
    public Mono<ResponseEntity<Mono<Map>>> getStockOMS(@RequestParam String itemId, @RequestParam String facId) {
        return WebClient
                .create(baseUrlOms)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/stock/v1/item/{item-id}/{fac-id}")
                        .build(itemId, facId))
                .exchange()
                .map(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return ResponseEntity.status(clientResponse.statusCode()).body(clientResponse.bodyToMono(Map.class));
                    } else{
                        //Aqui se tratarían los errores, por ejemplo, hacemos que devuelva un 409 para todos los errores
                        // y le añadimos al body un AtributoDeError
                        Mono<Map> mapMono = clientResponse.bodyToMono(Map.class).doOnSuccess(m -> m.put("AtributoDeError", "Error llamando al api"));
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(mapMono);
                    }
                });
    }
}
