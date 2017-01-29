@RestController
class ThisWillActuallyRun {

        @RequestMapping("/linebot/callback")
        String home() {
            return app.sources(Application.class);
            }

    }