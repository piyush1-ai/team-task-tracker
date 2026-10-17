package com.axionix.team_task_tracker.exception;

public record ApiError(ErrorBody error) {
    public record ErrorBody(String code , String message){}
    public static ApiError of(String code , String message){
        return new ApiError(new ErrorBody(code , message));
    }
}
