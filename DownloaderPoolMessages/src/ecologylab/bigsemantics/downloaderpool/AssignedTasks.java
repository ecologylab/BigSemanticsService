package ecologylab.bigsemantics.downloaderpool;

import java.util.List;

import ecologylab.serialization.annotations.simpl_collection;

/**
 * Convenient message class that encloses multiple tasks.
 * 
 * @author quyin
 */
public class AssignedTasks
{

  @simpl_collection("task")
  private List<Task> tasks;

  public List<Task> getTasks()
  {
    return tasks;
  }

  public void setTasks(List<Task> tasks)
  {
    this.tasks = tasks;
  }
  
  public static AssignedTasks EMPTY_ASSIGNMENT;
  
  static
  {
    EMPTY_ASSIGNMENT = new AssignedTasks();
  }

}
