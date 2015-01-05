package ecologylab.bigsemantics.downloaderpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;

import ecologylab.bigsemantics.downloaderpool.Task.State;

/**
 * A set of tests with the controller.
 * 
 * @author quyin
 */
public class TestController
{

  /**
   * The controller instance.
   */
  Controller ctrl;

  @Before
  public void init() throws ConfigurationException
  {
    Configuration configs = new PropertiesConfiguration("dpool-testing.properties");
    ctrl = new Controller(configs);
  }

  @Test
  public void testConstruction()
  {
    assertNotNull(ctrl);
  }

  @Test
  public void testAddTask()
  {
    Task task = new Task("my-task-0", "http://example.com");
    // when the task is created, its state is INIT.
    assertEquals(State.INIT, task.getState());
    ctrl.queueTask(task);
    // after queuing it to the controller, the controller should see 1 waiting task, and the task's
    // state should become WAITING.
    assertEquals(1, ctrl.countWaitingTasks());
    assertEquals(State.WAITING, task.getState());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddNullTask()
  {
    // you cannot queue null tasks
    ctrl.queueTask(null);
  }

  @Test
  public void testAddTasksWithSameUri()
  {
    Task task1 = new Task("my-task-1", "http://example.com");
    ctrl.queueTask(task1);
    Task task2 = new Task("my-task-2", "http://example.com");
    ctrl.queueTask(task2);
    // when you queue two tasks with the same URL, they should be merged into one task.
    // the merged task should contain clients for both of the origin ones.
    // the state of the 2nd task should become DEDUP.
    assertEquals(1, ctrl.countWaitingTasks());
    assertEquals(State.DEDUP, task2.getState());
  }

  @Test
  public void testGetTasksForWork()
  {
    String uri1 = "http://example.com";
    String uri2 = "http://www.mydomain.com/index.html";
    String uri3 = "http://lib.resources.com/resource/1";
    String uri4 = "http://book.mylibrary.com/search?q=java";

    Task task1 = new Task("my-task-1", uri1);
    ctrl.queueTask(task1);
    Task task2 = new Task("my-task-2", uri2);
    ctrl.queueTask(task2);
    Task task3 = new Task("my-task-3", uri3);
    ctrl.queueTask(task3);
    Task task4 = new Task("my-task-4", uri4);
    ctrl.queueTask(task4);

    DownloaderRequest req = new DownloaderRequest();
    // the blacklist contains domains that the downloader cannot work with at this moment.
    req.addToBlacklist("example.com");
    req.addToBlacklist("mydomain.com");
    req.setMaxTaskCount(10);

    List<Task> tasks = ctrl.getTasksForWork(req);
    assertNotNull(tasks);
    // when you use blacklist, tasks with domains in the blacklist should not be retrieved and
    // returned to the downloader.
    assertEquals(2, tasks.size());
    assertEquals(uri3, tasks.get(0).getUri());
    assertEquals(uri4, tasks.get(1).getUri());
    assertEquals(2, ctrl.countOngoingTasks());
    // the two tasks with blacklisted domains are waiting, while the other two are being downloaded.
    assertEquals(State.WAITING, task1.getState());
    assertEquals(State.WAITING, task2.getState());
    assertEquals(State.ONGOING, task3.getState());
    assertEquals(State.ONGOING, task4.getState());
  }

  @Test
  public void testCountDownTasks()
  {
    Task task = new Task("my-task-0", "http://example.com");
    task.setAttemptTime(200); // each attempt should be within 200ms
    task.setMaxAttempts(2); // 2 attempts at most
    ctrl.queueTask(task);

    // t = 0ms from beginning
    DownloaderRequest req = new DownloaderRequest();
    req.setMaxTaskCount(1);
    ctrl.getTasksForWork(req);
    assertEquals(1, ctrl.countOngoingTasks());

    ctrl.countDownTasks(100);
    // t = 100ms from beginning: still in the 1st attempt
    assertEquals(100, task.getTimer());
    assertEquals(1, ctrl.countOngoingTasks());
    assertEquals(0, ctrl.countWaitingTasks());

    ctrl.countDownTasks(100);
    // t = 200ms from beginning: first attempt failed
    assertEquals(200, task.getTimer()); // task timed out, moved to waiting queue
    assertEquals(0, ctrl.countOngoingTasks());
    assertEquals(1, ctrl.countWaitingTasks());

    ctrl.getTasksForWork(req); // task reassigned to a downloader

    ctrl.countDownTasks(100);
    // t = 100ms from beginning of the 2nd attempt: still in the 2nd attempt
    assertEquals(100, task.getTimer());
    assertEquals(1, ctrl.countOngoingTasks());
    assertEquals(0, ctrl.countWaitingTasks());

    ctrl.countDownTasks(100);
    // t = 200ms from beginning of the 2nd attempt: 2nd attempt failed
    assertEquals(0, task.getTimer()); // maximum attempts reached; task terminated
    assertEquals(0, ctrl.countOngoingTasks());
    assertEquals(0, ctrl.countWaitingTasks());

    assertEquals(State.TERMINATED, task.getState());
  }

}
