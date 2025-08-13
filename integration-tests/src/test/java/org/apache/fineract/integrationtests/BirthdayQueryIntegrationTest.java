package org.apache.fineract.integrationtests;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsStatusChecker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for querying Savings Accounts by birthday.
 */
@SuppressWarnings({ "rawtypes" })
@Order(2)
public class BirthdayQueryIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(BirthdayQueryIntegrationTest.class);
    public static final String MINIMUM_OPENING_BALANCE = "1000.0";
    public static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private SavingsAccountHelper savingsAccountHelper;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    /**
     * Happy Path: Verifies that a savings account is returned when querying with a valid birthday.
     */
    @Test
    public void testSavingsAccounts_withBirthdayQueryFilter_shouldReturnCorrectAccount() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        // 1. Create a client with a specific date of birth
        final String dateOfBirth = "15 January 1990";
        final Integer clientId = ClientHelper.createClientWithDateOfBirth(this.requestSpec, this.responseSpec,
                ClientHelper.DEFAULT_DATE, ClientHelper.DEFAULT_OFFICE_ID, dateOfBirth);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId);

        // 2. Create and activate a savings account for this client
        final String enforceMinRequiredBalance = "false";
        final boolean allowOverdraft = false;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE,
                null, null, enforceMinRequiredBalance, allowOverdraft);
        assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap modifications = this.savingsAccountHelper.updateSavingsAccount(clientId, savingsProductID, savingsId,
                ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertTrue(modifications.containsKey("submittedOnDate"));

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);
        this.savingsAccountHelper.calculateInterestForSavings(savingsId);
        HashMap summary = this.savingsAccountHelper.getSavingsSummary(savingsId);
        assertEquals(summaryBefore, summary);

        this.savingsAccountHelper.postInterestForSavings(savingsId);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId);
        assertNotEquals(summaryBefore, summary);

        // 3. Query for savings accounts filtered by the birthday date
        String queryParam = "birthday=01-15";
        String response = this.savingsAccountHelper.getSavingsAccountsWithQueryParam(queryParam);
        List<Map> savingsAccounts = this.savingsAccountHelper.parseJson(response); // <-- CHANGE THIS LINE

// 4. Assert the response contains the correct client's savings account
        assertNotNull(savingsAccounts);
        assertEquals(1, savingsAccounts.size(), "Expected one savings account for the specified birthday.");
        assertEquals(Double.valueOf(clientId), savingsAccounts.get(0).get("clientId"), "Expected client ID to match the client with birthday Jan 15.");
    }

    /**
     * Happy Path (No Match): Verifies that no savings accounts are returned when the birthday filter does not match any client.
     */
    @Test
    public void testSavingsAccounts_withNonMatchingBirthdayQuery_shouldReturnEmptyList() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        // 1. Create a client with a birthday that won't match the query
        final String dateOfBirth = "16 January 1990";
        final Integer clientId = ClientHelper.createClientWithDateOfBirth(this.requestSpec, this.responseSpec,
                ClientHelper.DEFAULT_DATE, ClientHelper.DEFAULT_OFFICE_ID, dateOfBirth);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId);

        // 2. Create and activate a savings account for this client
        final String enforceMinRequiredBalance = "false";
        final boolean allowOverdraft = false;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE,
                null, null, enforceMinRequiredBalance, allowOverdraft);
        assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap modifications = this.savingsAccountHelper.updateSavingsAccount(clientId, savingsProductID, savingsId,
                ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertTrue(modifications.containsKey("submittedOnDate"));

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);
        this.savingsAccountHelper.calculateInterestForSavings(savingsId);
        HashMap summary = this.savingsAccountHelper.getSavingsSummary(savingsId);
        assertEquals(summaryBefore, summary);

        this.savingsAccountHelper.postInterestForSavings(savingsId);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId);
        assertNotEquals(summaryBefore, summary);

        // 3. Query for savings accounts using a different birthday date
        String queryParam = "birthday=01-17";
        String response = this.savingsAccountHelper.getSavingsAccountsWithQueryParam(queryParam);
        List<Map> savingsAccounts = this.savingsAccountHelper.parseJson(response);

        // 4. Assert the response is an empty list
        assertNotNull(savingsAccounts);
        assertTrue(savingsAccounts.isEmpty(), "Expected an empty list of accounts for a non-matching birthday.");
    }

    /**
     * Sad Path: Verifies that a malformed date of birth query throws an IllegalArgumentException.
     */
    @Test
    public void testSavingsAccounts_withInvalidBirthdayFormat_shouldThrowException() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        // Create a response spec that expects a 400 Bad Request
        ResponseSpecification badRequestResponseSpec = new ResponseSpecBuilder().expectStatusCode(400).build();

        // 1. Create a client to ensure there's data in the system
        final Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId);

        // 2. Define the invalid date of birth query parameter
        String invalidQueryParam = "birthday=January%2015th";

        // 3. Assert that calling the API with this parameter returns a 400 Bad Request
        this.savingsAccountHelper.getSavingsAccountsWithQueryParamExpectingError(invalidQueryParam, badRequestResponseSpec);
    }

    /**
     * Sad Path: Verifies that a impossible date of birth query throws an IllegalArgumentException.
     */
    @Test
    public void testSavingsAccounts_withImpossibleBirthday_shouldThrowException() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        // Create a response spec that expects a 400 Bad Request
        ResponseSpecification badRequestResponseSpec = new ResponseSpecBuilder().expectStatusCode(400).build();

        // 1. Create a client to ensure there's data in the system
        final Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId);

        // 2. Define the invalid date of birth query parameter
        String invalidQueryParam = "birthday=02-30";

        // 3. Assert that calling the API with this parameter returns a 400 Bad Request
        this.savingsAccountHelper.getSavingsAccountsWithQueryParamExpectingError(invalidQueryParam, badRequestResponseSpec);
    }

    /**
     * Happy Path (Multiple Clients): Verifies that all accounts are returned when multiple clients share the same birthday.
     */
    @Test
    public void testSavingsAccounts_withMultipleClientsSharingBirthday_shouldReturnAllAccounts() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        // 1. Create two clients with the same birthday
        final String commonBirthday = "10 February 1995";
        final Integer clientId_A = ClientHelper.createClientWithDateOfBirth(this.requestSpec, this.responseSpec,
                ClientHelper.DEFAULT_DATE, ClientHelper.DEFAULT_OFFICE_ID, commonBirthday);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId_A);
        final Integer clientId_B = ClientHelper.createClientWithDateOfBirth(this.requestSpec, this.responseSpec,
                ClientHelper.DEFAULT_DATE, ClientHelper.DEFAULT_OFFICE_ID, commonBirthday);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId_B);

        // 2. Create and activate savings accounts for both clients
        final String enforceMinRequiredBalance = "false";
        final boolean allowOverdraft = false;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE,
                null, null, enforceMinRequiredBalance, allowOverdraft);
        assertNotNull(savingsProductID);

        final Integer savingsId_A = this.savingsAccountHelper.applyForSavingsApplication(clientId_A, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap modifications = this.savingsAccountHelper.updateSavingsAccount(clientId_A, savingsProductID, savingsId_A,
                ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertTrue(modifications.containsKey("submittedOnDate"));

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId_A);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId_A);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId_A);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore_A = this.savingsAccountHelper.getSavingsSummary(savingsId_A);
        this.savingsAccountHelper.calculateInterestForSavings(savingsId_A);
        HashMap summary = this.savingsAccountHelper.getSavingsSummary(savingsId_A);
        assertEquals(summaryBefore_A, summary);

        this.savingsAccountHelper.postInterestForSavings(savingsId_A);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId_A);
        assertNotEquals(summaryBefore_A, summary);

        final Integer savingsId_B = this.savingsAccountHelper.applyForSavingsApplication(clientId_B, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        modifications = this.savingsAccountHelper.updateSavingsAccount(clientId_B, savingsProductID, savingsId_B,
                ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertTrue(modifications.containsKey("submittedOnDate"));

        savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId_B);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId_B);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId_B);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore_B = this.savingsAccountHelper.getSavingsSummary(savingsId_B);
        this.savingsAccountHelper.calculateInterestForSavings(savingsId_B);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId_B);
        assertEquals(summaryBefore_B, summary);

        this.savingsAccountHelper.postInterestForSavings(savingsId_B);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId_B);
        assertNotEquals(summaryBefore_B, summary);

        // 3. Query for savings accounts using the common birthday
        String queryParam = "birthday=02-10";
        String response = this.savingsAccountHelper.getSavingsAccountsWithQueryParam(queryParam);
        List<Map> savingsAccounts = this.savingsAccountHelper.parseJson(response);

        // 4. Assert the response contains both clients' accounts
        assertNotNull(savingsAccounts);
        assertEquals(2, savingsAccounts.size(), "Expected two accounts for the clients sharing a birthday.");

        List<Integer> returnedClientIds = savingsAccounts.stream()
                .map(account -> ((Number) account.get("clientId")).intValue())
                .toList();

        assertTrue(returnedClientIds.contains(clientId_A));
        assertTrue(returnedClientIds.contains(clientId_B));
    }

    /**
     * Happy Path (Multiple Clients): Verifies that all accounts are returned when multiple clients share the same birthday but on different years.
     */
    @Test
    public void testSavingsAccounts_withMultipleClientsSharingBirthdayButDifferentYears_shouldReturnAllAccounts() {
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        // 1. Create two clients with the same birthday
        final String clientBirthday_A = "23 September 1995";
        final String clientBirthday_B = "23 September 2002";
        final Integer clientId_A = ClientHelper.createClientWithDateOfBirth(this.requestSpec, this.responseSpec,
                ClientHelper.DEFAULT_DATE, ClientHelper.DEFAULT_OFFICE_ID, clientBirthday_A);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId_A);
        final Integer clientId_B = ClientHelper.createClientWithDateOfBirth(this.requestSpec, this.responseSpec,
                ClientHelper.DEFAULT_DATE, ClientHelper.DEFAULT_OFFICE_ID, clientBirthday_B);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId_B);

        // 2. Create and activate savings accounts for both clients
        final String enforceMinRequiredBalance = "false";
        final boolean allowOverdraft = false;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE,
                null, null, enforceMinRequiredBalance, allowOverdraft);
        assertNotNull(savingsProductID);

        final Integer savingsId_A = this.savingsAccountHelper.applyForSavingsApplication(clientId_A, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap modifications = this.savingsAccountHelper.updateSavingsAccount(clientId_A, savingsProductID, savingsId_A,
                ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertTrue(modifications.containsKey("submittedOnDate"));

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId_A);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId_A);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId_A);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore_A = this.savingsAccountHelper.getSavingsSummary(savingsId_A);
        this.savingsAccountHelper.calculateInterestForSavings(savingsId_A);
        HashMap summary = this.savingsAccountHelper.getSavingsSummary(savingsId_A);
        assertEquals(summaryBefore_A, summary);

        this.savingsAccountHelper.postInterestForSavings(savingsId_A);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId_A);
        assertNotEquals(summaryBefore_A, summary);

        final Integer savingsId_B = this.savingsAccountHelper.applyForSavingsApplication(clientId_B, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        modifications = this.savingsAccountHelper.updateSavingsAccount(clientId_B, savingsProductID, savingsId_B,
                ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertTrue(modifications.containsKey("submittedOnDate"));

        savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId_B);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId_B);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId_B);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore_B = this.savingsAccountHelper.getSavingsSummary(savingsId_B);
        this.savingsAccountHelper.calculateInterestForSavings(savingsId_B);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId_B);
        assertEquals(summaryBefore_B, summary);

        this.savingsAccountHelper.postInterestForSavings(savingsId_B);
        summary = this.savingsAccountHelper.getSavingsSummary(savingsId_B);
        assertNotEquals(summaryBefore_B, summary);

        // 3. Query for savings accounts using the common birthday
        String queryParam = "birthday=09-23";
        String response = this.savingsAccountHelper.getSavingsAccountsWithQueryParam(queryParam);
        List<Map> savingsAccounts = this.savingsAccountHelper.parseJson(response);

        // 4. Assert the response contains both clients' accounts
        assertNotNull(savingsAccounts);
        assertEquals(2, savingsAccounts.size(), "Expected two accounts for the clients sharing a birthday but on different years.");

        List<Integer> returnedClientIds = savingsAccounts.stream()
                .map(account -> ((Number) account.get("clientId")).intValue())
                .toList();

        assertTrue(returnedClientIds.contains(clientId_A));
        assertTrue(returnedClientIds.contains(clientId_B));
    }

    private Integer createSavingsProduct(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
                                         final String minOpenningBalance, String minBalanceForInterestCalculation, String minRequiredBalance,
                                         String enforceMinRequiredBalance, final boolean allowOverdraft) {
        final String taxGroupId = null;
        return createSavingsProduct(requestSpec, responseSpec, minOpenningBalance, minBalanceForInterestCalculation, minRequiredBalance,
                enforceMinRequiredBalance, allowOverdraft, taxGroupId, false);
    }

    private Integer createSavingsProduct(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
                                         final String minOpenningBalance, String minBalanceForInterestCalculation, String minRequiredBalance,
                                         String enforceMinRequiredBalance, final boolean allowOverdraft, final String taxGroupId, boolean withDormancy) {
        LOG.info("------------------------------CREATING NEW SAVINGS PRODUCT ---------------------------------------");
        SavingsProductHelper savingsProductHelper = new SavingsProductHelper();
        if (allowOverdraft) {
            final String overDraftLimit = "2000.0";
            savingsProductHelper = savingsProductHelper.withOverDraft(overDraftLimit);
        }
        if (withDormancy) {
            savingsProductHelper = savingsProductHelper.withDormancy();
        }

        final String savingsProductJSON = savingsProductHelper
                //
                .withInterestCompoundingPeriodTypeAsDaily()
                //
                .withInterestPostingPeriodTypeAsMonthly()
                //
                .withInterestCalculationPeriodTypeAsDailyBalance()
                //
                .withMinBalanceForInterestCalculation(minBalanceForInterestCalculation)
                //
                .withMinRequiredBalance(minRequiredBalance).withEnforceMinRequiredBalance(enforceMinRequiredBalance)
                .withMinimumOpenningBalance(minOpenningBalance).withWithHoldTax(taxGroupId).build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }
}