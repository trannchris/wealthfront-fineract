package org.apache.fineract.portfolio.savings.api;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SavingsAccountsApiResourceTest {

    private SavingsAccountsApiResource savingsAccountsApiResource;

    @Mock
    private SavingsAccountReadPlatformService savingsAccountReadPlatformService;
    @Mock
    private PlatformSecurityContext context;
    @Mock
    private DefaultToApiJsonSerializer<SavingsAccountData> toApiJsonSerializer;
    @Mock
    private ApiRequestParameterHelper apiRequestParameterHelper;

    @BeforeEach
    public void setUp() {
        // Initialize the resource with mocked dependencies
        this.savingsAccountsApiResource = new SavingsAccountsApiResource(
                savingsAccountReadPlatformService,
                context,
                toApiJsonSerializer,
                null,
                apiRequestParameterHelper,
                null,
                null,
                null
        );

        // Create a mock object for the AppUser
        AppUser mockAppUser = mock(AppUser.class);

        // Chain the mock calls properly
        // Tell the context mock to return the mockAppUser
        when(context.authenticatedUser()).thenReturn(mockAppUser);

        // Mock the security permission check to succeed without throwing an exception.
        doNothing().when(mockAppUser).validateHasReadPermission(any());
    }

    /**
     * Happy Path: Verify that a valid birthday parameter is correctly passed to the service layer.
     */
    @Test
    void testRetrieveAll_withValidBirthdayParameter() {
        // Given
        final String expectedBirthday = "01-15";
        final UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        when(savingsAccountReadPlatformService.retrieveAll(any(SearchParameters.class)))
                .thenReturn(new Page<>(new ArrayList<SavingsAccountData>(), 0));

        // When
        savingsAccountsApiResource.retrieveAll(uriInfo, null, null, null, null, null, null, expectedBirthday);

        // Then
        ArgumentCaptor<SearchParameters> captor = ArgumentCaptor.forClass(SearchParameters.class);
        verify(savingsAccountReadPlatformService).retrieveAll(captor.capture());

        SearchParameters capturedSearchParameters = captor.getValue();
        assertEquals(expectedBirthday, capturedSearchParameters.getBirthday(), "The birthday parameter should be correctly passed.");
    }

    /**
     * Happy Path: Verify that the birthday parameter does not interfere with other valid parameters.
     */
    @Test
    void testRetrieveAll_withBirthdayAndOtherParameters() {
        // Given
        final String expectedBirthday = "01-15";
        final String sqlSearch = "client.name='John Doe'";
        final Integer limit = 50;
        final String orderBy = "id";

        final UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        when(savingsAccountReadPlatformService.retrieveAll(any(SearchParameters.class)))
                .thenReturn(new Page<>(new ArrayList<SavingsAccountData>(), 0));

        // When
        savingsAccountsApiResource.retrieveAll(uriInfo, sqlSearch, null, null, limit, orderBy, null, expectedBirthday);

        // Then
        ArgumentCaptor<SearchParameters> captor = ArgumentCaptor.forClass(SearchParameters.class);
        verify(savingsAccountReadPlatformService).retrieveAll(captor.capture());

        SearchParameters capturedSearchParameters = captor.getValue();
        assertEquals(expectedBirthday, capturedSearchParameters.getBirthday(), "The birthday parameter should be correctly passed.");
        assertEquals(sqlSearch, capturedSearchParameters.getSqlSearch(), "Other parameters should not be affected.");
        assertEquals(limit, capturedSearchParameters.getLimit(), "Limit parameter should be correctly passed.");
        assertEquals(orderBy, capturedSearchParameters.getOrderBy(), "OrderBy parameter should be correctly passed.");
    }

    /**
     * Happy Path: Verify that the birthday parameter is null when not provided.
     */
    @Test
    void testRetrieveAll_withoutBirthdayParameter() {
        // Given
        final UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        when(savingsAccountReadPlatformService.retrieveAll(any(SearchParameters.class)))
                .thenReturn(new Page<>(new ArrayList<SavingsAccountData>(), 0));

        // When
        savingsAccountsApiResource.retrieveAll(uriInfo, null, null, null, null, null, null, null);

        // Then
        ArgumentCaptor<SearchParameters> captor = ArgumentCaptor.forClass(SearchParameters.class);
        verify(savingsAccountReadPlatformService).retrieveAll(captor.capture());

        SearchParameters capturedSearchParameters = captor.getValue();
        assertNull(capturedSearchParameters.getBirthday(), "The birthday parameter should be null when not provided.");
    }

    /**
     * Happy Path (Edge Case): Verify that an empty birthday parameter is correctly handled.
     */
    @Test
    void testRetrieveAll_withEmptyBirthdayParameter() {
        // Given
        final UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        when(savingsAccountReadPlatformService.retrieveAll(any(SearchParameters.class)))
                .thenReturn(new Page<>(new ArrayList<SavingsAccountData>(), 0));

        // When
        // The API resource receives a blank string, which is then passed to the SearchParameters
        savingsAccountsApiResource.retrieveAll(uriInfo, null, null, null, null, null, null, "");

        // Then
        ArgumentCaptor<SearchParameters> captor = ArgumentCaptor.forClass(SearchParameters.class);
        verify(savingsAccountReadPlatformService).retrieveAll(captor.capture());

        SearchParameters capturedSearchParameters = captor.getValue();
        assertEquals("", capturedSearchParameters.getBirthday(), "An empty birthday string should be passed correctly.");
    }

    /**
     * Sad Path (Edge Case): Verify that an invalid birthday format is correctly handled.
     */
    @Test
    void testRetrieveAll_withInvalidBirthdayFormat() {
        // Given
        final String invalidBirthday = "January 15th";
        final UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        when(savingsAccountReadPlatformService.retrieveAll(any(SearchParameters.class)))
                .thenThrow(new IllegalArgumentException("Invalid 'birthday' format. Use MM-dd."));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            savingsAccountsApiResource.retrieveAll(uriInfo, null, null, null, null, null, null, invalidBirthday);
        });

        ArgumentCaptor<SearchParameters> captor = ArgumentCaptor.forClass(SearchParameters.class);
        verify(savingsAccountReadPlatformService).retrieveAll(captor.capture());
        SearchParameters capturedSearchParameters = captor.getValue();
        assertEquals(invalidBirthday, capturedSearchParameters.getBirthday(), "An invalid birthday string should be passed correctly.");
    }

    /**
     * Sad Path: Verifies that an invalid day for a month (e.g., February 30th)
     * correctly throws an IllegalArgumentException.
     */
    @Test
    void testRetrieveAll_withInvalidDayOfMonth() {
        // Given
        final String invalidBirthday = "02-30";
        final UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);

        when(savingsAccountReadPlatformService.retrieveAll(any(SearchParameters.class)))
                .thenThrow(new IllegalArgumentException("Invalid 'birthday' format. Use MM-dd."));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            savingsAccountsApiResource.retrieveAll(uriInfo, null, null, null, null, null, null, invalidBirthday);
        });

        ArgumentCaptor<SearchParameters> captor = ArgumentCaptor.forClass(SearchParameters.class);
        verify(savingsAccountReadPlatformService).retrieveAll(captor.capture());
        SearchParameters capturedSearchParameters = captor.getValue();
        assertEquals(invalidBirthday, capturedSearchParameters.getBirthday(), "An invalid day of month string should be passed correctly.");
    }

}