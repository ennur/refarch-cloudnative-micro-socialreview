
package socialreview.cloudant;

import com.google.gson.Gson;
import com.linecorp.bot.client.LineMessagingServiceBuilder;
import com.linecorp.bot.client.LineSignatureValidator;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.action.MessageAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.template.CarouselColumn;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.response.BotApiResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping(value="/linebot")
public class LineBotController
{
    @Autowired
    @Qualifier("com.linecorp.channel_secret")
    String lChannelSecret;
    
    @Autowired
    @Qualifier("com.linecorp.channel_access_token")
    String lChannelAccessToken;

    @RequestMapping(value="/callback", method=RequestMethod.POST)
    public ResponseEntity<String> callback(
        @RequestHeader("X-Line-Signature") String aXLineSignature,
        @RequestBody String aPayload)
    {
         // compose body
        final String text=String.format("The Signature is: %s",
            (aXLineSignature!=null && aXLineSignature.length() > 0) ? aXLineSignature : "N/A");
        
        System.out.println(text);
        
        final boolean valid=new LineSignatureValidator(lChannelSecret.getBytes()).validateSignature(aPayload.getBytes(), aXLineSignature);
        
        System.out.println("The signature is: " + (valid ? "valid" : "tidak valid"));
        
        //Get events from source
        if(aPayload!=null && aPayload.length() > 0)
        {
            System.out.println("Payload: " + aPayload);
        }
        
        Gson gson = new Gson();
        Payload payload = gson.fromJson(aPayload, Payload.class);
        
        //Variable initialization
        String msgText = " ";
        String upload_url = " ";
        String mJSON = " ";
        String idTarget = " ";
        String eventType = payload.events[0].type;
        
        //Get event's type
        if (eventType.equals("join")){
            if (payload.events[0].source.type.equals("group")){
                replyToUser(payload.events[0].replyToken, "Hello Group");
            }
            if (payload.events[0].source.type.equals("room")){
                replyToUser(payload.events[0].replyToken, "Hello Room");
            }
        } else if (eventType.equals("message")){    //Event's type is message
            if (payload.events[0].source.type.equals("group")){
                idTarget = payload.events[0].source.groupId;
            } else if (payload.events[0].source.type.equals("room")){
                idTarget = payload.events[0].source.roomId;
            } else if (payload.events[0].source.type.equals("user")){
                idTarget = payload.events[0].source.userId;
            }
            
            //Parsing message from user
            if (!payload.events[0].message.type.equals("text")){
                replyToUser(payload.events[0].replyToken, "Unknown message");
            } else {
                //Get movie data from OMDb API
                msgText = payload.events[0].message.text;
                msgText = msgText.toLowerCase();
                
                if (!msgText.contains("bot leave")){
                    try {
                        getMovieData(msgText, payload, idTarget);
                    } catch (IOException e) {
                        System.out.println("Exception is raised ");
                        e.printStackTrace();
                    }
                } else {
                    if (payload.events[0].source.type.equals("group")){
                        leaveGR(payload.events[0].source.groupId, "group");
                    } else if (payload.events[0].source.type.equals("room")){
                        leaveGR(payload.events[0].source.roomId, "room");
                    }
                }
                
//                pushType(idTarget, msgText + " - " + payload.events[0].source.type);
            }
        }
         
        return new ResponseEntity<String>(HttpStatus.OK);
    }
    
    //Method for get movie data from OMDb API
    private void getMovieData(String title, Payload ePayload, String targetID) throws IOException{
        String userTxt = title;

//        if (title.indexOf("\"") == -1){
//            replyToUser(ePayload.events[0].replyToken, "Unknown keyword");
//            return;
//        }
//
//        title = title.substring(title.indexOf("\"") + 1, title.lastIndexOf("\""));
//        System.out.println("Index: " + Integer.toString(title.indexOf("\"")));
//        title = title.replace(" ", "+");
//        System.out.println("Text from User: " + title);

        // Act as client with GET method
        String URI = "https://www.dicoding.com/public/api/events";
        System.out.println("URI: " +  URI);
        
        String jObjGet = " ";
        CloseableHttpAsyncClient c = HttpAsyncClients.createDefault();
        
        try{
            c.start();
            //Use HTTP Get to retrieve data
            HttpGet get = new HttpGet(URI);
            
            Future<HttpResponse> future = c.execute(get, null);
            HttpResponse responseGet = future.get();
            System.out.println("HTTP executed");
            System.out.println("HTTP Status of response: " + responseGet.getStatusLine().getStatusCode());
            
            // Get the response from the GET request
            BufferedReader brd = new BufferedReader(new InputStreamReader(responseGet.getEntity().getContent()));
            
            StringBuffer resultGet = new StringBuffer();
            String lineGet = "";
            while ((lineGet = brd.readLine()) != null) {
                resultGet.append(lineGet);
            }
            System.out.println("Got result");
            
            // Change type of resultGet to JSONObject
            jObjGet = resultGet.toString();
            System.out.println("OMDb responses: " + jObjGet);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        } finally {
            c.close();
        }
        
        Gson mGson = new Gson();
        Event event = mGson.fromJson(jObjGet, Event.class);
        String owner = event.getData().get(0).getOwner_display_name();
        String summary = event.getData().get(0).getSummary();
        String description = event.getData().get(0).getDescription();
        String link = event.getData().get(0).getLink();
        String time = event.getData().get(0).getBegin_time() + " - " + event.getData().get(0).getEnd_time();
        String address = event.getData().get(0).getAddress();
        String image = event.getData().get(0).getImage_path();
        String msgToUser = " ";
        
        //Check user's request
        if (userTxt.contains("name")){
            pushMessage(targetID, event.getData().get(0).getName());
        } else if (userTxt.contains("summary")){
            pushMessage(targetID, summary);
        } else if (userTxt.contains("description")){
            pushMessage(targetID, description);
        } else if (userTxt.contains("time")){
            pushMessage(targetID, time);
        } else if (userTxt.contains("address")){
            pushMessage(targetID, address);
        } else if (userTxt.contains("owner")){
            pushMessage(targetID, owner);
        }
        else if (userTxt.contains("event")){
            carouselForUser(image, ePayload.events[0].source.userId, owner, link);
        }
        
        System.out.println("Message to user: " + image);
        
//        //Check whether response successfully retrieve or not
//        if (msgToUser.length() <= 11 || !ePayload.events[0].message.type.equals("text")){
//            replyToUser(ePayload.events[0].replyToken, "Request Timeout");
//        } else {
//            replyToUser(ePayload.events[0].replyToken, msgToUser);
//        }
    }

    //Method for reply user's message
    private void replyToUser(String rToken, String messageToUser){
        TextMessage textMessage = new TextMessage(messageToUser);
        ReplyMessage replyMessage = new ReplyMessage(rToken, textMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(lChannelAccessToken)
                .build()
                .replyMessage(replyMessage)
                .execute();
            System.out.println("Reply Message: " + response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    //Method for send movie's poster to user
    private void pushPoster(String sourceId, String poster_url){
        ImageMessage imageMessage = new ImageMessage(poster_url, poster_url);
        PushMessage pushMessage = new PushMessage(sourceId,imageMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(lChannelAccessToken)
                .build()
                .pushMessage(pushMessage)
                .execute();
            System.out.println(response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    //Method for push message to user
    private void pushMessage(String sourceId, String txt){
        TextMessage textMessage = new TextMessage(txt);
        PushMessage pushMessage = new PushMessage(sourceId,textMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
            .create(lChannelAccessToken)
            .build()
            .pushMessage(pushMessage)
            .execute();
            System.out.println(response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    //Method for send caraousel template message to user
    private void carouselForUser(String poster_url, String sourceId, String title, String uri){
        CarouselTemplate carouselTemplate = new CarouselTemplate(
                    Arrays.asList(new CarouselColumn
                                    (poster_url, title, "Select one for more info", Arrays.asList
                                        (new MessageAction("Summary", "summary"),
                                         new MessageAction("Description", "description"),
                                         new URIAction("Link", uri))),
                                  new CarouselColumn
                                    (poster_url, title, "Select one for more info", Arrays.asList
                                        (new MessageAction("Time", "time"),
                                         new MessageAction("Address", "address"),
                                         new MessageAction("Owner", "owner")))));
        TemplateMessage templateMessage = new TemplateMessage("Your search result", carouselTemplate);
        PushMessage pushMessage = new PushMessage(sourceId,templateMessage);
        try {
            Response<BotApiResponse> response = LineMessagingServiceBuilder
                .create(lChannelAccessToken)
                .build()
                .pushMessage(pushMessage)
                .execute();
            System.out.println(response.code() + " " + response.message());
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
    
    //Method for leave group or room
    private void leaveGR(String id, String type){
        try {
            if (type.equals("group")){
                Response<BotApiResponse> response = LineMessagingServiceBuilder
                    .create(lChannelAccessToken)
                    .build()
                    .leaveGroup(id)
                    .execute();
                System.out.println(response.code() + " " + response.message());
            } else if (type.equals("room")){
                Response<BotApiResponse> response = LineMessagingServiceBuilder
                    .create(lChannelAccessToken)
                    .build()
                    .leaveRoom(id)
                    .execute();
                System.out.println(response.code() + " " + response.message());
            }
        } catch (IOException e) {
            System.out.println("Exception is raised ");
            e.printStackTrace();
        }
    }
}
