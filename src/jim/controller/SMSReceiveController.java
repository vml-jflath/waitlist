package jim.controller;

import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import jim.domain.Customer;
import jim.service.WaitListService;
import jim.util.SMSStaticMessageList;

@Path("/SMSService")
public class SMSReceiveController {

	@Context
	private HttpServletRequest servletRequest;

	static DecimalFormat df = new DecimalFormat("00.00");

	private WaitListService waitListService = new WaitListService();
	
	// http://localhost:8080/WaitList/rest/Service/receiveSMSMessage
	// https://www.twilio.com/docs/api/twiml/sms/twilio_request
	@POST
	@Path("/receiveSMSMessage/{location}")
	@Produces("application/xml")
	//ToCountry=US&ToState=MO&SmsMessageSid=SM99635d6fdd89d4d24ebb174d7ca0ec9b&NumMedia=0&ToCity=GALENA&FromZip=64015&SmsSid=SM99635d6fdd89d4d24ebb174d7ca0ec9b&FromState=MO&SmsStatus=received&FromCity=KANSAS+CITY&Body=xml+body+this+time&FromCountry=US&To=%2B18166563912&ToZip=65656&NumSegments=1&MessageSid=SM99635d6fdd89d4d24ebb174d7ca0ec9b&AccountSid=AC5324997ab13479ec75f3190681c28d72&From=%2B18166741125&ApiVersion=2010-04-01
	//Body=my message body
	//From=%2B18166741125
	public Response receiveSMSMessage( @PathParam("location")String location, @FormParam("Body")String body, @FormParam("From") String messageFrom, String x) {
	
		String phoneNumber = messageFrom.replace("+", "");
		String messageBody = body.trim();
		System.out.println("In the recieveSMSMessage method [" + x + "]");
		System.out.println("Body ["+messageBody+"]");
		System.out.println("From Number ["+ phoneNumber +"]");
		System.out.println("Location [" + location + "]");
		
		String messageResponse = "";
		if( "remove".equalsIgnoreCase(messageBody)){
			System.out.println("Going to delete this phone number");
			waitListService.deleteCustomerByPhone( phoneNumber, location );
			System.out.println("Done deleting");
			messageResponse = SMSStaticMessageList.getDeleteSMSResponse();

		} else if ("status".equalsIgnoreCase(messageBody) ){
			System.out.println("Checking status");
			messageResponse = SMSStaticMessageList.getStatusSMSResponse( waitListService.getCustomerByPhone( phoneNumber, location ));
			System.out.println("Done getting status");
		} else if( messageBody.toUpperCase().startsWith("ADD") || messageBody.startsWith("🔫") ){
			System.out.println( "Adding someone via text");
			Customer customer = parseSMSCustomerAdd( messageBody, phoneNumber, location );
			if( customer==null){
				messageResponse = SMSStaticMessageList.getSMSAddErrorResponse();
			} else {
				int position = waitListService.addCustomerToList( customer );
				messageResponse = SMSStaticMessageList.getSignupMessage(customer, position);
			}
		}
		return Response.status(200).entity("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response>"+messageResponse+"</Response>").build();
	}
	
	private Customer parseSMSCustomerAdd( String messageBody, String phoneNumber, String location ){
		String[] stringArray = messageBody.split(" ");
		try{
			return new Customer( stringArray[1], stringArray[2], phoneNumber, location); // TODO need to cross reference the TO NUMBER to a location
		} catch (Exception e ){
			e.printStackTrace();
			return null;
		}
	}
}
