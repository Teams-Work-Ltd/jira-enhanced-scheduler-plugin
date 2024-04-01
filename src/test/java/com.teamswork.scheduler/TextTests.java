package com.teamswork.scheduler;

import org.junit.Test;

import static com.teamswork.scheduler.utils.StringUtils.extractThreadGroupName;
import static org.junit.Assert.assertEquals;

public class TextTests {

    @Test
    public void willExtractThreadGroupName() {
        String threadGroupName = "Caesium-2 : Started";
        String actualThreadGroupName = extractThreadGroupName(threadGroupName);

        System.out.println(actualThreadGroupName);
        assertEquals("Caesium-2", actualThreadGroupName);


        threadGroupName = "Caesium-1 :";
        actualThreadGroupName = extractThreadGroupName(threadGroupName);

        System.out.println(actualThreadGroupName);
        assertEquals("Caesium-1", actualThreadGroupName);

        threadGroupName = "Caesium-1";
        actualThreadGroupName = extractThreadGroupName(threadGroupName);

        System.out.println(actualThreadGroupName);
        assertEquals("Caesium-1", actualThreadGroupName);

        threadGroupName = "Caesium-";
        actualThreadGroupName = extractThreadGroupName(threadGroupName);

        System.out.println(actualThreadGroupName);
        assertEquals("Caesium-", actualThreadGroupName);
    }
}