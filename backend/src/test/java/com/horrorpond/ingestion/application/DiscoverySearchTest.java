package com.horrorpond.ingestion.application;

import com.horrorpond.ingestion.client.SteamSearchList;
import com.horrorpond.ingestion.client.SteamSpyClient;
import com.horrorpond.ingestion.client.SteamStoreClient;
import com.horrorpond.ingestion.client.SteamTransientException;
import com.horrorpond.ingestion.repository.SteamAppSeedRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoverySearchTest {

    private final SteamStoreClient storeClient = mock(SteamStoreClient.class);
    private final DiscoveryService service = new DiscoveryService(mock(SteamSpyClient.class), storeClient,
            mock(SteamAppSeedRepository.class), mock(DiscoveryWriter.class), mock(IngestionJobRecorder.class));

    @Test
    void walksWholeHorrorListUntilShortPage() {
        when(storeClient.fetchHorrorSearchPage(eq(SteamSearchList.NEW_RELEASES), anyInt(), eq(100)))
                .thenAnswer(inv -> {
                    int start = inv.getArgument(1);
                    return start < 300 ? ids(start, 100) : ids(start, 40);
                });
        when(storeClient.fetchHorrorSearchPage(eq(SteamSearchList.POPULAR_UPCOMING), anyInt(), eq(100)))
                .thenReturn(List.of());

        assertThat(service.searchPages(SteamSearchList.NEW_RELEASES, DiscoveryService.HORROR_SEARCH_LIMIT)).hasSize(340);
    }

    @Test
    void keepsPagesReceivedBeforeAFailureInTheMiddle() {
        when(storeClient.fetchHorrorSearchPage(eq(SteamSearchList.NEW_RELEASES), anyInt(), eq(100)))
                .thenAnswer(inv -> {
                    int start = inv.getArgument(1);
                    if (start == 200) {
                        throw new SteamTransientException("search responded 502");
                    }
                    return ids(start, 100);
                });

        assertThat(service.searchPages(SteamSearchList.NEW_RELEASES, DiscoveryService.HORROR_SEARCH_LIMIT)).hasSize(200);
    }

    @Test
    void failureOnFirstPageStillFailsTheSource() {
        when(storeClient.fetchHorrorSearchPage(eq(SteamSearchList.NEW_RELEASES), eq(0), eq(100)))
                .thenThrow(new SteamTransientException("search responded 502"));

        assertThatThrownBy(() -> service.searchPages(SteamSearchList.NEW_RELEASES, DiscoveryService.HORROR_SEARCH_LIMIT))
                .isInstanceOf(SteamTransientException.class);
    }

    private static List<Integer> ids(int start, int count) {
        return IntStream.range(start, start + count).map(i -> 1_000_000 + i).boxed().toList();
    }
}
