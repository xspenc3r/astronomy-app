package edu.umdearborn.astronomyapp.service;

import java.util.List;
import java.util.Map;

import edu.umdearborn.astronomyapp.entity.Answer;
import edu.umdearborn.astronomyapp.entity.CourseUser;
import edu.umdearborn.astronomyapp.entity.Module;
import edu.umdearborn.astronomyapp.entity.ModuleGroup;
import edu.umdearborn.astronomyapp.util.json.JsonDecorator;

public interface GroupService {

  public List<CourseUser> joinGroup(String courseUserId, String moduleId, String groupId);

  public ModuleGroup createGroup(String courseUserId, String moduleId);

  public boolean isInAGroup(String courseUserId, String moduleId);

  public List<CourseUser> getUsersInGroup(String groupId);

  public ModuleGroup getGroup(String courseUserId, String moduleId);

  public CourseUser checkin(String email, String password, String groupId);

  public boolean hasLock(String groupId, List<String> checkedIn);

  public List<Answer> saveAnswers(Map<String, Map<String, String>> answers, String groupId);

  public Long submissionNumber(String groupId);

  public List<Answer> getAnswers(String groupId, boolean getSavedAnswers);

  public void finalizeGroup(String groupId);

  public List<Answer> submitAnswers(String groupId);

  public List<CourseUser> removeFromGroup(String groupId, String courseUserId, String removeUserId);

  public List<CourseUser> getFreeUsers(String courseId, String moduleId);

  public List<String>  getGroups(String moduleId);
  
  public Map<String, List<CourseUser>> getGroupsWithMembers(String moduleId);

  public List<Answer> gradeAnswers(Map<String, Map<String, String>> answers);

  public JsonDecorator<String> getGroupModuleDetails(String groupId);

}
