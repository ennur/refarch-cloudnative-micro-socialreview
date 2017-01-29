@RestController
class ThisWillActuallyRun {

        @RequestMapping("/")
        SpringApplicationBuilder configure(SpringApplicationBuilder app)
        {
            return app.sources(Application.class);
        }

    }