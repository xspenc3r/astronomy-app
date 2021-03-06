
function Controller($scope, $state, $stateParams, appSettings, AssignmentService, QuestionService, ConfirmationService, AlertService){
    "ngInject";
    this._$state = $state;
    this.pageName = "Add/Edit Assignment Questions";
    this.courseId = $stateParams.courseId;
    this.moduleId = $stateParams.moduleId;
    this.pageNum = $stateParams.pageNum;
    this.success = $stateParams.success;
    this.questionTypes = appSettings.QUESTION_TYPES;
    this._$stateParams  = $stateParams;
    this._AssignmentService = AssignmentService;
    this._QuestionService = QuestionService;
    this._ConfirmationService = ConfirmationService;
    this._AlertService = AlertService;
    this.selectedQuestionType = "MULTIPLE_CHOICE";
    this.questions = [];
    this.init(); 
};

Controller.prototype.init = function(){
    var self = this;
    self.getQuestions();
}

Controller.prototype.getQuestions = function() {
    var self = this;
    self._QuestionService.getQuestions(self.courseId, self.moduleId, self.pageNum)
    .then(function(payload){
        self.questions = payload;
    }, function(err){
        self.error = "ERROR getting questions";
    });
};

Controller.prototype.dropQuestion = function(questionId) {
    var self = this;
    var confirmation = "Are you sure you want to delete the Question?";
    var footNote = "Once deleted, you can't get it back. ";
    var modalInstance = self._ConfirmationService.open("", confirmation, footNote);
    modalInstance.result.then(function(){
        self._QuestionService.dropQuestion(self.courseId, self.moduleId, questionId)
        .then(function(payload){
            self.questions = payload;
        }, function(err){
            self.error = "ERROR deleting the question";
        });
    }, function(){
        console.log("They said no");
    });
}

Controller.prototype.createPagesStruct = function(number){
    var pages = [];
    for(var i = 1; i<=number; i++){
        pages.push(i);
    }
    return pages;
}



Controller.prototype.reorderQuestion = function(itemId, newOrder) {
    var self = this;
    self._QuestionService.reorderQuestion(self.courseId, self.moduleId, itemId, newOrder)
    .then(function(payload){
    	
    }, function(err){
    	if(err.data.exception.endsWith("CustomException")) {
    		self.error = err.data.message;
    	} else {
    		self.error = "ERROR reordering question!";
    	}
        self.getQuestions();
    });
};

Controller.prototype.addQuestion = function(){
    var self = this;
    if(self.selectedQuestionType){
        var params = {
            moduleId : self.moduleId,
            pageNum : self.pageNum,
            questionId : "new",
            questionType : self.selectedQuestionType,
            isNew : true,
            questionData : {}
        };
        self._$state.go('app.course.assignments_add_edit_question', params, {reload: true});
    }
};

Controller.prototype.editQuestion = function(questionData){
    var self = this;
    var questionType = ("questionType" in questionData ? questionData.questionType : questionData.pageItemType);
    var params = {
        moduleId : self.moduleId,
        pageNum : self.pageNum,
        questionId : questionData.id,
        questionType : questionType,
        questionData : questionData
    };
    self._$state.go('app.course.assignments_add_edit_question', params, {reload : true});
};

Controller.prototype.dropped = function(event, index, item) {
    // Return false here to cancel drop. Return true if you insert the item yourself.
	var self = this;
	self.reorderQuestion(item.id, index + 1);
    return item;
};

module.exports = angular.module('app.views.instructor.questions.add_edit', [])
.controller('Instructor.QuestionsAddEdit', Controller);
