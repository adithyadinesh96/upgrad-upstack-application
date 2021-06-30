package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
import org.upgrad.upstac.testrequests.lab.LabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.users.User;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class LabRequestControllerTest {


    @InjectMocks
    LabRequestController labRequestController;


    @Mock
    TestRequestQueryService testRequestQueryService;

    @Mock
    UserLoggedInService userLoggedInService;

    @Mock
    TestRequestUpdateService testRequestUpdateService;

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("someuser");
        return user;
    }

    private TestRequest createMockTestRequest(RequestStatus status) {
        TestRequest mockTestRequest = new TestRequest();
        mockTestRequest.requestId = Long.MAX_VALUE;
        mockTestRequest.setStatus(status);
        return mockTestRequest;
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){

        //Arrange
        User user = createUser();
        TestRequest mockTestRequest = createMockTestRequest(RequestStatus.INITIATED);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest);

        TestRequest mockTestRequest2 = createMockTestRequest(RequestStatus.LAB_TEST_IN_PROGRESS);
        LabResult mockLabResult = new LabResult();
        mockLabResult.setResult(TestStatus.NEGATIVE);
        mockTestRequest2.setLabResult(mockLabResult);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.INITIATED)).thenReturn(lstTestRequest);
        Mockito.when(testRequestUpdateService.assignForLabTest(mockTestRequest.getRequestId(), user)).thenReturn(mockTestRequest2);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);
        TestRequest updatedTestRequest = labRequestController.assignForLabTest(testRequest.getRequestId());
        //Implement this method

        //Assert
        assertThat(testRequest.getRequestId(), equalTo(updatedTestRequest.getRequestId()));
        assertThat(updatedTestRequest.getStatus(), equalTo(RequestStatus.LAB_TEST_IN_PROGRESS));
        assertNotNull(updatedTestRequest.getLabResult());
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){
        //Arrange
        Long InvalidRequestId= -34L;
        User user= createUser();

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.assignForLabTest(InvalidRequestId, user)).thenThrow(new AppException("Invalid ID"));

        //Assert
        ResponseStatusException result = assertThrows(ResponseStatusException.class,()->{
            labRequestController.assignForLabTest(InvalidRequestId);
        });
        assertThat(result.getReason(), equalTo("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){
        //Arrange
        User user = createUser();
        TestRequest mockTestRequest1 = createMockTestRequest(RequestStatus.LAB_TEST_IN_PROGRESS);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest1);

        CreateLabResult createLabResult = getCreateLabResult(mockTestRequest1);
        TestRequest updatedMockTestRequest = createMockTestRequest(RequestStatus.LAB_TEST_COMPLETED);
        LabResult mockLabResult = createMockLabResult();
        updatedMockTestRequest.setLabResult(mockLabResult);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.LAB_TEST_IN_PROGRESS)).thenReturn(lstTestRequest);
        Mockito.when(testRequestUpdateService.updateLabTest(mockTestRequest1.getRequestId(), createLabResult, user)).thenReturn(updatedMockTestRequest);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        TestRequest updatedTestRequest = labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);

        //Assert
        assertThat(updatedMockTestRequest.getRequestId(), equalTo(updatedTestRequest.getRequestId()));
        assertThat(updatedMockTestRequest.getStatus(), equalTo(RequestStatus.LAB_TEST_COMPLETED));
        assertThat(updatedMockTestRequest.getLabResult(), equalTo(updatedTestRequest.getLabResult()));
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){
        //Arrange
        Long InvalidRequestId= -34L;
        User user= createUser();
        TestRequest mockTestRequest1 = createMockTestRequest(RequestStatus.LAB_TEST_IN_PROGRESS);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest1);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.LAB_TEST_IN_PROGRESS)).thenReturn(lstTestRequest);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        Mockito.when(testRequestUpdateService.updateLabTest(InvalidRequestId, createLabResult, user)).thenThrow(new AppException("Invalid ID"));


        //Assert
        ResponseStatusException result = assertThrows(ResponseStatusException.class,()->{
            labRequestController.updateLabTest(InvalidRequestId, createLabResult);
        });
        assertThat(result.getReason(), equalTo("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){
        //Arrange
        User user= createUser();
        TestRequest mockTestRequest1 = createMockTestRequest(RequestStatus.LAB_TEST_IN_PROGRESS);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest1);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.LAB_TEST_IN_PROGRESS)).thenReturn(lstTestRequest);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = getCreateLabResult(testRequest);
        createLabResult.setResult(null);
        Mockito.when(testRequestUpdateService.updateLabTest(testRequest.getRequestId(), createLabResult, user)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConstraintViolationException"));


        //Assert
        ResponseStatusException result = assertThrows(ResponseStatusException.class,()->{
            labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);
        });
        assertThat(result.getReason(), containsString("ConstraintViolationException"));
    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {
        CreateLabResult mockCreateLabResult = new CreateLabResult();
        mockCreateLabResult.setResult(TestStatus.POSITIVE);
        mockCreateLabResult.setComments("Take Rest");
        mockCreateLabResult.setBloodPressure("120");
        mockCreateLabResult.setHeartBeat("91");
        mockCreateLabResult.setTemperature("100");
        mockCreateLabResult.setOxygenLevel("92");
        return mockCreateLabResult;
    }

    private LabResult createMockLabResult() {
        LabResult labResult = new LabResult();
        labResult.setResult(TestStatus.POSITIVE);
        labResult.setComments("Take Rest");
        labResult.setBloodPressure("120");
        labResult.setHeartBeat("91");
        labResult.setTemperature("100");
        labResult.setOxygenLevel("92");
        return labResult;
    }

}