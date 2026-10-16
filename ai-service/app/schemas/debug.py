from typing import Literal

from pydantic import BaseModel, Field


class CodeDebugRequest(BaseModel):
    module: Literal["matching", "offers", "interviews", "profile"]
    language: Literal["python", "java", "javascript", "typescript"]
    code: str = Field(min_length=1, max_length=20000)
    expected_behavior: str = Field(default="", max_length=2000)
