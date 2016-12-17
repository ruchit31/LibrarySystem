/**
 * 
 */
package edu.sjsu.cmpe275.term.controller;

/**
 * @author Ajeet
 *
 */

import java.util.Date;
import org.springframework.scheduling.annotation.Scheduled;
 
public class DemoServiceBasicUsageCron
{
    @Scheduled(cron="*/60 * * * * ?")
    public void demoServiceMethod()
    {	/*System.out.println("Am in being called twice");
        System.out.println("Method executed at every 5 seconds. Current time is :: "+ new Date());*/
    }
}
