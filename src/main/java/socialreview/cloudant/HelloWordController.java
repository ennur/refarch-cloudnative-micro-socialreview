package socialreview.cloudant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Created by root on 1/29/17.
 */
@RestController
public class HelloWordController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public String home() {
        return "OK";
    }

    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public String hello() {
        return "hello world";
    }


}
