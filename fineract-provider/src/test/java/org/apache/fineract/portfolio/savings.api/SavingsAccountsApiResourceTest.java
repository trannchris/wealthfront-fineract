package org.apache.fineract.portfolio.savings.api;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

}