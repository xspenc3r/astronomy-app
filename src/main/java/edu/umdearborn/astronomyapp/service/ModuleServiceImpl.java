package edu.umdearborn.astronomyapp.service;

import static edu.umdearborn.astronomyapp.entity.PageItem.PageItemType.QUESTION;
import static edu.umdearborn.astronomyapp.entity.PageItem.PageItemType.TEXT;
import static edu.umdearborn.astronomyapp.entity.Question.QuestionType.MULTIPLE_CHOICE;
import static edu.umdearborn.astronomyapp.entity.Question.QuestionType.NUMERIC;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.hibernate.annotations.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.umdearborn.astronomyapp.controller.exception.CustomException;
import edu.umdearborn.astronomyapp.controller.exception.UpdateException;
import edu.umdearborn.astronomyapp.entity.Course;
import edu.umdearborn.astronomyapp.entity.Module;
import edu.umdearborn.astronomyapp.entity.MultipleChoiceOption;
import edu.umdearborn.astronomyapp.entity.MultipleChoiceQuestion;
import edu.umdearborn.astronomyapp.entity.NumericQuestion;
import edu.umdearborn.astronomyapp.entity.Page;
import edu.umdearborn.astronomyapp.entity.PageItem;
import edu.umdearborn.astronomyapp.entity.PageItem.PageItemType;
import edu.umdearborn.astronomyapp.entity.Question;
import edu.umdearborn.astronomyapp.entity.Question.QuestionType;
import edu.umdearborn.astronomyapp.entity.UnitOption;
import edu.umdearborn.astronomyapp.util.ResultListUtil;
import edu.umdearborn.astronomyapp.util.json.JsonDecorator;

@Service
@Transactional
public class ModuleServiceImpl implements ModuleService {

  private static final Logger logger = LoggerFactory.getLogger(ModuleServiceImpl.class);

  private EntityManager entityManager;

  public ModuleServiceImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Module createModule(Module module) {
    entityManager.persist(module);
    return module;
  }

  @Override
  public Module updateModule(Module module) {
    entityManager.merge(module);
    return module;
  }
  
  @Override
  public PageItem updatePageItem(PageItem item) {
  TypedQuery<Boolean> query =
	        entityManager.createQuery("select count (m) > 0 from Module m where m.id = :moduleId and "
	            + "m.openTimestamp <= current_timestamp()", Boolean.class);
	    query.setParameter("moduleId", item.getPage().getModule().getId());
  boolean moduleOpen = query.getSingleResult();
	  
  if (PageItemType.QUESTION.equals(item.getPageItemType())) {
      if (QuestionType.MULTIPLE_CHOICE.equals(((Question) item).getQuestionType())) {
	  MultipleChoiceQuestion question = (MultipleChoiceQuestion) item;
      Set<MultipleChoiceOption> newOptions = question.getOptions();
      
      for(MultipleChoiceOption o : newOptions) {
    	  logger.info("Updating question to {} for option {}", question.getId(), o.getId());
      	o.setQuestion(question.getId());
      }
	    
      if(!moduleOpen) {
	      MultipleChoiceQuestion currentQuestion = entityManager.find(MultipleChoiceQuestion.class, item.getId());
	      Set<MultipleChoiceOption> currentOptions = currentQuestion.getOptions();
	      
	      HashMap<String, MultipleChoiceOption> newOptionsMap = new HashMap<String, MultipleChoiceOption>();
	      for (MultipleChoiceOption newOpt: newOptions) {
	    	  if(newOpt.getId() != null) {
	    		  newOptionsMap.put(newOpt.getId(), newOpt);
	    	  }
	      }
	      
	      for(MultipleChoiceOption oldOption : currentOptions) {
			  if(!newOptionsMap.containsKey(oldOption.getId())) {
				  logger.info("Removing option {}", oldOption.getId());
				    entityManager
			        .createNativeQuery(
			            "delete from multiple_choice_option where multiple_choice_option_id = ?")
			        .setParameter(1, oldOption.getId()).executeUpdate();
			  }
	      }
	      entityManager.flush();
      }
      
      logger.info("Merging");
      entityManager.merge(question);
        
      } else if (QuestionType.NUMERIC.equals(((Question) item).getQuestionType())) {
    	  NumericQuestion question = (NumericQuestion) item;
          Set<UnitOption> newOptions = question.getOptions();
          
          for(UnitOption o : newOptions) {
        	  logger.info("Updating question to {} for option {}", question.getId(), o.getId());
          	o.setQuestion(question.getId());
          }
    	  
          if(!moduleOpen) {
	          NumericQuestion currentQuestion = entityManager.find(NumericQuestion.class, item.getId());
	          Set<UnitOption> currentOptions = currentQuestion.getOptions();
	          
	          HashMap<String, UnitOption> newOptionsMap = new HashMap<String, UnitOption>();
	          for (UnitOption newOpt: newOptions) {
	        	  if(newOpt.getId() != null) {
	        		  newOptionsMap.put(newOpt.getId(), newOpt);
	        	  }
	          }
	          
	          for(UnitOption oldOption : currentOptions) {
	    		  if(!newOptionsMap.containsKey(oldOption.getId())) {
	    			  logger.info("Removing option {}", oldOption.getId());
	    			    entityManager
	    		        .createNativeQuery(
	    		            "delete from unit_option where unit_option_id = ?")
	    		        .setParameter(1, oldOption.getId()).executeUpdate();
	    		  }
	          }
	          entityManager.flush();
          }
          
          logger.info("Merging");
          entityManager.merge(question);
      } else {
    	  logger.info("Merging");
          entityManager.merge(item);
      }
    } else {
    	logger.info("Merging");
        entityManager.merge(item);
    }
  
    return item;
  }

  @Override
  public List<Module> getModules(String courseId, boolean showVisibleOnly) {
    StringBuilder jpql =
        new StringBuilder("select m from Module m join m.course c where c.id = :courseId");

    if (showVisibleOnly) {
      logger.debug("Hiding not yet visible modules");
      jpql.append(" and m.openTimestamp <= current_timestamp()");
    }

    logger.debug("Resuting JPQL: {}", jpql.toString());
    TypedQuery<Module> query = entityManager.createQuery(jpql.toString(), Module.class);
    query.setParameter("courseId", courseId);

    return query.getResultList();
  }

  @Override
  public JsonDecorator<Module> getModuleDetails(String moduleId) {

    TypedQuery<Module> moduleQuery =
        entityManager.createQuery("select m from Module m where m.id = :moduleId", Module.class);
    EntityGraph<Module> entityGraph = entityManager.createEntityGraph(Module.class);
    entityGraph.addAttributeNodes("humanReadableText");
    moduleQuery.setHint(QueryHints.FETCHGRAPH, entityGraph);
    moduleQuery.setParameter("moduleId", moduleId);
    List<Module> resultList = moduleQuery.getResultList();

    if (ResultListUtil.hasResult(resultList)) {
      JsonDecorator<Module> decorator = new JsonDecorator<>();
      decorator.setPayload(resultList.get(0));

      TypedQuery<Long> pageCountQuery = entityManager.createQuery(
          "select count(p) from Page p join p.module m where m.id = :moduleId", Long.class);
      pageCountQuery.setParameter("moduleId", moduleId);
      decorator.addProperty("numPages", pageCountQuery.getSingleResult());

      TypedQuery<Long> questionCountQuery =
          entityManager.createQuery("select count(q) from Question q join q.page p join "
              + "p.module m where m.id = :moduleId", Long.class);
      questionCountQuery.setParameter("moduleId", moduleId);
      decorator.addProperty("numQuestion", questionCountQuery.getSingleResult());

      decorator.addProperty("points", getMaxPoints(moduleId));

      return decorator;
    }

    logger.debug("Not results for module: '{}'", moduleId);
    return null;

  }

  @Override
  public void reorderPageItem(String itemId, int newOrder) {
	  PageItem pageItem = entityManager.find(PageItem.class, itemId);
	
		//valid page, valid new position for page
	    if(pageItem != null && newOrder > 0) {
	    	
		TypedQuery<PageItem> itemsQuery = entityManager.createQuery(
		        "select i from PageItem i where i.page.id = :pageId order by i.order asc",
		        PageItem.class)
			  .setParameter("pageId", pageItem.getPage().getId());
		List<PageItem> pageItems = itemsQuery.getResultList();
		
		  int case1 = 0;
		  int case2 = 0;
		  int shift = 0;
		  
		  if(pageItem.getOrder() > newOrder) {
			  case1 = newOrder - 1;
			  case2 = pageItem.getOrder();
			  shift = 1;
		  }else if(pageItem.getOrder() < newOrder) {
			  case1 = pageItem.getOrder();
			  case2 = newOrder;
			  shift = -1;
		  }
		  
		  final int fcase1 = case1;
		  final int fcase2 = case2;
		  
		  List<PageItem> reorder = pageItems;
		  if(pageItem.getOrder() > newOrder) {
			  //avoid duplicate order numbers
			  reorder = pageItems.stream()
	      						.sorted(Comparator.comparing(PageItem::getOrder).reversed())
	      						.filter(item -> item.getOrder() > fcase1)
	      						.filter(item -> item.getOrder() < fcase2)
	      						.collect(Collectors.toList());
		  }else {
			  reorder = pageItems.stream()
						.filter(item -> item.getOrder() > fcase1)
						.filter(item -> item.getOrder() < fcase2)
						.collect(Collectors.toList());
			  newOrder --; //make sure item is above selected
			  if(reorder.size() > 0 && reorder.get(reorder.size() - 1).getOrder() < newOrder) {
				  //any number above maximum order number will reset to last item order
				  System.out.println("Moving as last element");
				  newOrder = reorder.get(reorder.size() - 1).getOrder();
			  }
		  }
		  
		  if(reorder.size() > 0) {
			    
			  int tempOrder = pageItems.get(pageItems.size() - 1).getOrder() + 1;
			  
			  //move pageItem out of the way (max order number + 1)
			  pageItem.setOrder(tempOrder);
			  entityManager.flush();
			  
			  //shift certain pageItems up or down one
			  for(PageItem p : reorder) {
			    	p.setOrder(p.getOrder() + shift);
			    	entityManager.flush();
			    }
			  
			  //move pageItem to new position
			  pageItem.setOrder(newOrder);
			  entityManager.flush();
		  } else {
			  //nothing to reorder, must be the case order==newOrder or order is last item and trying to move down
			  System.out.println("Nothing to do");
			  throw new CustomException("Nothing to reorder");
		  }
		  
	  } else {
		  if(pageItem == null) {
			  throw new CustomException("Item with id: " + itemId + " does not exist");
		  } else {
			  throw new CustomException("Invalid order number");
		  }
	  }
  }
  
  @Override
  public List<PageItem> getPage(String moduleId, int pageNumber) {

    TypedQuery<PageItem> textPageItemQuery = entityManager.createQuery(
        "select i from PageItem i join i.page p join p.module m where m.id = :moduleId and "
            + "p.order = :pageNumber and i.pageItemType = :textType",
        PageItem.class);
    textPageItemQuery.setParameter("moduleId", moduleId).setParameter("pageNumber", pageNumber)
        .setParameter("textType", TEXT);
    List<PageItem> result = textPageItemQuery.getResultList();

    if (!ResultListUtil.hasResult(result)) {
      result = new ArrayList<>();
    }

    TypedQuery<Question> questionPageItemQuery = entityManager.createQuery(
        "select q from Question q join q.page p join p.module m where m.id = :moduleId and "
            + "p.order = :pageNumber and q.pageItemType = :questionType and q.questionType not "
            + "in :machineGradeable",
        Question.class);
    questionPageItemQuery.setParameter("moduleId", moduleId).setParameter("pageNumber", pageNumber)
        .setParameter("questionType", QUESTION)
        .setParameter("machineGradeable", Arrays.asList(MULTIPLE_CHOICE, NUMERIC));
    List<Question> questionResult = questionPageItemQuery.getResultList();

    if (ResultListUtil.hasResult(questionResult)) {
      result.addAll(questionResult);
    }

    TypedQuery<MultipleChoiceQuestion> multipleChoicePageItemQuery = entityManager
        .createQuery("select q from MultipleChoiceQuestion q join q.page p join p.module m where"
            + " m.id = :moduleId and p.order = :pageNumber", MultipleChoiceQuestion.class);
    multipleChoicePageItemQuery.setParameter("moduleId", moduleId).setParameter("pageNumber",
        pageNumber);
    List<MultipleChoiceQuestion> multipleChoiceResult = multipleChoicePageItemQuery.getResultList();

    if (ResultListUtil.hasResult(multipleChoiceResult)) {
      result.addAll(multipleChoiceResult);
    }

    TypedQuery<NumericQuestion> numericPageItemQuery = entityManager
        .createQuery("select q from NumericQuestion q join q.page p join p.module m where"
            + " m.id = :moduleId and p.order = :pageNumber", NumericQuestion.class);
    numericPageItemQuery.setParameter("moduleId", moduleId).setParameter("pageNumber", pageNumber);
    List<NumericQuestion> numericResult = numericPageItemQuery.getResultList();

    if (ResultListUtil.hasResult(numericResult)) {
      result.addAll(numericResult);
    }

    result = result.stream()
		.sorted(Comparator.comparing(PageItem::getOrder))
		.collect(Collectors.toList());
    
    return result;
  }

  @Override
  public BigDecimal getMaxPoints(String moduleId) {

    TypedQuery<BigDecimal> query = entityManager
        .createQuery("select coalesce(sum(q.points), 0) from Question q join q.page p join "
            + "p.module m where m.id = :moduleId", BigDecimal.class);
    query.setParameter("moduleId", moduleId);
    
    TypedQuery<BigDecimal> query2 = entityManager
            .createQuery("select coalesce(sum(nq.unitPoints), 0) from NumericQuestion nq, Question q join q.page p join "
                + "p.module m where m.id = :moduleId and q.id = nq.id", BigDecimal.class);
        query2.setParameter("moduleId", moduleId);

    return query.getSingleResult().add(query2.getSingleResult());
  }

  @Override
  public void deletePage(String moduleId, int pageNumber) {
    List<Page> results = entityManager
        .createQuery(
            "select p from Page p join p.module m where m.id = :moduleId and p.order = :pageNumber",
            Page.class)
        .setParameter("moduleId", moduleId).setParameter("pageNumber", pageNumber).getResultList();

    if (ResultListUtil.hasResult(results)) {
      entityManager.remove(results.get(0));

      logger.debug("Re-ordering remaining pages");
      entityManager
          .createQuery("update Page p set p.order = p.order - 1 "
              + "where p.order > :pageNumber and p.module.id = :moduleId")
          .setParameter("pageNumber", pageNumber).setParameter("moduleId", moduleId)
          .executeUpdate();
    }

  }

  @Override
  public int addPage(String moduleId) {
    int order = entityManager
        .createQuery("select count(p) + 1 from Page p join p.module m where m.id = :moduleId",
            Long.class)
        .setParameter("moduleId", moduleId).getSingleResult().intValue();

    Module module = new Module();
    module.setId(moduleId);

    Page page = new Page();
    page.setOrder(order);
    page.setModule(module);

    entityManager.persist(page);
    return page.getOrder();
  }

  @Override
  public PageItem createPageItem(PageItem item, String moduleId, int pageNum) {
    item.prePersist();

    List<String> results = entityManager
        .createQuery(
            "select p.id from Page p join p.module m where m.id = :moduleId and p.order = :pageNum",
            String.class)
        .setParameter("moduleId", moduleId).setParameter("pageNum", pageNum).getResultList();

    if (ResultListUtil.hasResult(results)) {
      insertPageItem(item, results.get(0));

      if (PageItemType.QUESTION.equals(item.getPageItemType())) {
        insertQuestion((Question) item);

        if (QuestionType.MULTIPLE_CHOICE.equals(((Question) item).getQuestionType())) {
          insertMultipleChoice((MultipleChoiceQuestion) item);
        } else if (QuestionType.NUMERIC.equals(((Question) item).getQuestionType())) {
          insertNumeric((NumericQuestion) item);
        }
      }

      return item;

    } else {
      logger.error("Page number: '{}' does not exist for module: '{}'", pageNum, moduleId);

      throw new UpdateException(
          "Cannot insert item into page number: " + pageNum + " for in module: " + moduleId);
    }
  }

  private void insertPageItem(PageItem item, String pageId) {
    logger.debug("Inserting into page_item");

    entityManager
        .createNativeQuery(
            "insert into page_item(page_item_id, page_item_type, item_order, human_readable_text, "
                + "page_id) values (?, ?, "
                + "(select coalesce(max(item_order), 0) + 1 from page_item where page_id = ?), ?, ?)")
        .setParameter(1, item.getId()).setParameter(2, item.getPageItemType().toString())
        .setParameter(3, pageId).setParameter(4, item.getHumanReadableText())
        .setParameter(5, pageId).executeUpdate();
  }

  private void insertQuestion(Question question) {
    logger.debug("Inserting into question");
    entityManager
        .createNativeQuery(
            "insert into question(question_id, question_type, default_comment, is_gatekeeper, points) "
                + "values (?, ?, ?, ?, ?)")
        .setParameter(1, question.getId()).setParameter(2, question.getQuestionType().toString())
        .setParameter(3, question.getDefaultComment()).setParameter(4, question.isGatekeeper())
        .setParameter(5, question.getPoints()).executeUpdate();
  }

  private void insertMultipleChoice(MultipleChoiceQuestion question) {
    logger.debug("Inserting into multiple_choice_question");
    entityManager
        .createNativeQuery(
            "insert into multiple_choice_question( multiple_choice_question_id) values (?)")
        .setParameter(1, question.getId()).executeUpdate();

    logger.debug("Inserting into multiple_choice_question options");

    StringBuilder builder = new StringBuilder(
        "insert into multiple_choice_option(multiple_choice_option_id, is_correct_option, "
            + "human_readable_text, option_question_id) values");
    List<Object> params = question.getOptions().stream().flatMap(o -> {
      builder.append("(?, ?, ?, ?),");
      o.prePersist();
      return Arrays
          .asList(o.getId(), o.isCorrectOption(), o.getHumanReadableText(), question.getId())
          .stream();
    }).collect(Collectors.toList());

    Query query =
        entityManager.createNativeQuery(builder.deleteCharAt(builder.length() - 1).toString());

    IntStream.rangeClosed(1, params.size()).forEach(i -> query.setParameter(i, params.get(--i)));

    query.executeUpdate();
  }

  private void insertNumeric(NumericQuestion question) {
    logger.debug("Inserting into numeric_question");
    entityManager
        .createNativeQuery(
            "insert into numeric_question(numeric_question_id, allowed_coefficient_spread, "
                + "allowed_exponenet_spread, correct_coefficient, correct_exponenet, requires_scale, unit_points) "
                + "values (?, ?, ?, ?, ?, ?, ?)")
        .setParameter(1, question.getId()).setParameter(2, question.getAllowedCoefficientSpread())
        .setParameter(3, question.getAllowedExponenetSpread())
        .setParameter(4, question.getCorrectCoefficient())
        .setParameter(5, question.getCorrectExponenet()).setParameter(6, question.getRequiresScale())
        .setParameter(7, question.getUnitPoints())
        .executeUpdate();

    logger.debug("Inserting into numeric_question options");
    
    if(question.getOptions().size() > 0) {
	    StringBuilder builder = new StringBuilder(
	        "insert into unit_option(unit_option_id, is_correct_option, option_question_id, "
	            + "help_text, human_readable_text) values");
	    List<Object> params = question.getOptions().stream().flatMap(o -> {
	      builder.append("(?, ?, ?, ?, ?),");
	      o.prePersist();
	      return Arrays.asList(o.getId(), o.isCorrectOption(), question.getId(), o.getHelpText(),
	          o.getHumanReadableText()).stream();
	    }).collect(Collectors.toList());
	
	    Query query =
	        entityManager.createNativeQuery(builder.deleteCharAt(builder.length() - 1).toString());
	
	    IntStream.rangeClosed(1, params.size()).forEach(i -> query.setParameter(i, params.get(--i)));
	
	    query.executeUpdate();
    }
    
  }

  @Override
  public List<PageItem> deletePageItem(String moduleId, String pageItemId) {
    List<PageItem> results = entityManager
        .createQuery("select item from PageItem item where item.id = :id", PageItem.class)
        .setParameter("id", pageItemId).getResultList();

    if (ResultListUtil.hasResult(results)) {
      int pageNum = results.get(0).getPage().getOrder();
      entityManager.remove(results.get(0));
      return getPage(moduleId, pageNum);
    } else {
      throw new UpdateException("Item with id: " + pageItemId + " does not exist");
    }
  }

  @Override
  public void deleteModule(String moduleId) {
    entityManager.remove(entityManager.find(Module.class, moduleId));

  }

  @Override
  public void ensureValidDates(String courseId, Module module) {
    Course course = entityManager.getReference(Course.class, courseId);
    if (course.getCloseTimestamp() == null || course.getOpenTimestamp() == null
        || module.getCloseTimestamp() == null || module.getOpenTimestamp() == null
        || course.getCloseTimestamp().before(module.getCloseTimestamp())
        || course.getOpenTimestamp().after(module.getOpenTimestamp())) {
      throw new UpdateException("Module's open and close timestamp must be within the course");
    }
  }
}
