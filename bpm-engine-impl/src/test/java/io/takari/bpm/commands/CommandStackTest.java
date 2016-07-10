package io.takari.bpm.commands;

import static org.junit.Assert.*;
import org.junit.Test;

import io.takari.bpm.commands.Command;
import io.takari.bpm.commands.CommandStack;

public class CommandStackTest {

    @Test
    public void testSimple() throws Exception {
        TestCommand a = new TestCommand();
        TestCommand b = new TestCommand();
        TestCommand c = new TestCommand();
        
        // ---
        
        CommandStack s = new CommandStack();
        s = s.push(a);
        s = s.push(b);
        s = s.push(c);
        
        // ---
        
        assertEquals(c, s.peek());
        
        s.pop();
        assertEquals(c, s.peek());
        
        s = s.pop();
        assertEquals(b, s.peek());
        
        s = s.pop();
        assertEquals(a, s.peek());
    }
    
    private static final class TestCommand implements Command {

        private static final long serialVersionUID = 1L;
    }
}
