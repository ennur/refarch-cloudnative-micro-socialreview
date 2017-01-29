@RestController
class ThisWillActuallyRun {

    @Autowired
    @Qualifier("com.linecorp.channel_secret")
    String lChannelSecret

    @Autowired
    @Qualifier("com.linecorp.channel_access_token")
    String lChannelAccessToken

    @RequestMapping("/callback")
    public ResponseEntity<String> callback(
            @RequestHeader("X-Line-Signature") String aXLineSignature,
            @RequestBody String aPayload)
    {
        // compose body

        return new ResponseEntity<String>(HttpStatus.OK)
    }

    }