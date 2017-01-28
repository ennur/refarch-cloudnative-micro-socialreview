@RestController
class ThisWillActuallyRun {

    @RequestMapping("/")
    String home() {
        return "Hello World!"
    }

    @RequestMapping("/hello")
    String hello() {
        return "Hello!"
    }

}
