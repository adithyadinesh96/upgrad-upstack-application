package org.upgrad.upstac.testrequests;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.exception.AppException;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.testrequests.consultation.Consultation;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.testrequests.RequestStatus;
import org.upgrad.upstac.testrequests.TestRequestQueryService;
import org.upgrad.upstac.users.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Slf4j
class ConsultationControllerTest {


    @InjectMocks
    ConsultationController consultationController;


    @Mock
    TestRequestQueryService testRequestQueryService;

    @Mock
    UserLoggedInService userLoggedInService;

    @Mock
    TestRequestUpdateService testRequestUpdateService;

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status(){

        //Arrange
        User user = createUser();
        TestRequest mockTestRequest = createMockTestRequest(RequestStatus.LAB_TEST_COMPLETED);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest);
        TestRequest mockTestRequest1 = createMockTestRequest(RequestStatus.DIAGNOSIS_IN_PROCESS);
        Consultation mockConsultation = createMockConsultation(user, mockTestRequest1);
        mockTestRequest1.setConsultation(mockConsultation);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.LAB_TEST_COMPLETED)).thenReturn(lstTestRequest);
        Mockito.when(testRequestUpdateService.assignForConsultation(Long.MAX_VALUE, user)).thenReturn(mockTestRequest1);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);
        TestRequest testRequest1 = consultationController.assignForConsultation(testRequest.requestId);


        //Assert
        assertThat(testRequest.requestId, equalTo(testRequest1.requestId));
        assertThat(testRequest1.getStatus(), equalTo(RequestStatus.DIAGNOSIS_IN_PROCESS));
        assertNotNull(testRequest1.getConsultation());

    }

    private Consultation createMockConsultation(User user, TestRequest mockTestRequest1) {
        Consultation mockConsultation = new Consultation();
        mockConsultation.setRequest(mockTestRequest1);
        mockConsultation.setComments("Good");
        mockConsultation.setDoctor(user);
        mockConsultation.setId(Long.MAX_VALUE);
        mockConsultation.setUpdatedOn(LocalDate.now());
        return mockConsultation;
    }

    private TestRequest createMockTestRequest(RequestStatus status) {
        TestRequest mockTestRequest = new TestRequest();
        mockTestRequest.requestId = Long.MAX_VALUE;
        mockTestRequest.setStatus(status);
        return mockTestRequest;
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("someuser");
        return user;
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception(){

        //Arrange
        Long InvalidRequestId= -34L;
        User user= createUser();

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestUpdateService.assignForConsultation(InvalidRequestId, user)).thenThrow(new AppException("Invalid ID"));

        //Assert
        ResponseStatusException result = assertThrows(ResponseStatusException.class,()->{
            consultationController.assignForConsultation(InvalidRequestId);
        });
        assertThat(result.getReason(), equalTo("Invalid ID"));
        //Implement this method


        // Create an object of ResponseStatusException . Use assertThrows() method and pass assignForConsultation() method
        // of consultationController with InvalidRequestId as Id


        //Use assertThat() method to perform the following comparison
        //  the exception message should be contain the string "Invalid ID"

    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details(){
        //Arrange
        User user = createUser();
        TestRequest mockTestRequest = createMockTestRequest(RequestStatus.DIAGNOSIS_IN_PROCESS);
        TestRequest mockTestRequest1 = createMockTestRequest(RequestStatus.COMPLETED);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.DIAGNOSIS_IN_PROCESS)).thenReturn(lstTestRequest);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest testResult = getCreateConsultationRequest(testRequest);
        mockTestRequest1.setConsultation(createMockConsultation(user, testRequest));
        Mockito.when(testRequestUpdateService.updateConsultation(mockTestRequest1.getRequestId(), testResult, user)).thenReturn(mockTestRequest1);
        TestRequest testRequestUpdated = consultationController.updateConsultation(mockTestRequest1.requestId, testResult);

        //Assert
        assertThat(mockTestRequest1.getRequestId(), equalTo(testRequestUpdated.getRequestId()));
        assertThat(testRequestUpdated.getStatus(), equalTo(RequestStatus.COMPLETED));
        assertThat(mockTestRequest1.getConsultation().getSuggestion(), equalTo(testRequestUpdated.getConsultation().getSuggestion()));

    }


    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception(){


        Long InvalidRequestId= -34L;
        User user = createUser();
        TestRequest mockTestRequest = createMockTestRequest(RequestStatus.LAB_TEST_COMPLETED);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.DIAGNOSIS_IN_PROCESS)).thenReturn(lstTestRequest);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest testResult = getCreateConsultationRequest(testRequest);
        Mockito.when(testRequestUpdateService.updateConsultation(InvalidRequestId, testResult, user)).thenThrow(new AppException("Invalid ID"));

        //Assert
        ResponseStatusException result = assertThrows(ResponseStatusException.class,()->{
            consultationController.updateConsultation(InvalidRequestId, testResult);
        });
        assertThat(result.getReason(), equalTo("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception(){
        //Arrange
        User user = createUser();
        TestRequest mockTestRequest = createMockTestRequest(RequestStatus.DIAGNOSIS_IN_PROCESS);
        List<TestRequest> lstTestRequest = new ArrayList<>();
        lstTestRequest.add(mockTestRequest);

        //Act
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        Mockito.when(testRequestQueryService.findBy(RequestStatus.DIAGNOSIS_IN_PROCESS)).thenReturn(lstTestRequest);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest testResult = getCreateConsultationRequest(testRequest);
        testResult.setSuggestion(null);
        Mockito.when(testRequestUpdateService.updateConsultation(testRequest.getRequestId(), testResult, user)).thenThrow(new AppException("Invalid ID"));

        //Assert
        ResponseStatusException result = assertThrows(ResponseStatusException.class,()->{
            consultationController.updateConsultation(testRequest.getRequestId(), testResult);
        });
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        CreateLabResult mockCreateLabResult = createMockLamResult();
        CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();
        if(mockCreateLabResult.getResult() == TestStatus.POSITIVE) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
            createConsultationRequest.setComments("HOME QUARANTINE");
        }
        else {
            createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
            createConsultationRequest.setComments("Ok");
        }
        return createConsultationRequest;

        //Create an object of CreateLabResult and set all the values
        // if the lab result test status is Positive, set the doctor suggestion as "HOME_QUARANTINE" and comments accordingly
        // else if the lab result status is Negative, set the doctor suggestion as "NO_ISSUES" and comments as "Ok"
        // Return the object

    }

    private CreateLabResult createMockLamResult() {
        CreateLabResult mockCreateLabResult = new CreateLabResult();
        mockCreateLabResult.setResult(TestStatus.POSITIVE);
        mockCreateLabResult.setComments("Take Rest");
        mockCreateLabResult.setBloodPressure("120");
        mockCreateLabResult.setHeartBeat("91");
        mockCreateLabResult.setTemperature("100");
        mockCreateLabResult.setOxygenLevel("92");
        return mockCreateLabResult;
    }

}