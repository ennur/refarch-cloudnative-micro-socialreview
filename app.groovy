@SpringBootApplication
public class Application extends SpringBootServletInitializer
{
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder app)
    {
        return app.sources(Application.class);
    }

    public static void main(String [] args)
    {
        SpringApplication.run(Application.class, args);
    }
}