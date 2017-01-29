@RestController
class ThisWillActuallyRun {

        @RequestMapping("/linebot/callback")
        String home() {
                return "Hello World!"
            }

    }