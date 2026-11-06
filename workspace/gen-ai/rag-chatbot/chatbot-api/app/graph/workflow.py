"""LangGraph 워크플로우 구성 (PRD 3.2.2, F2-6, F2-7).

START → classify ─(urban_planning)→ retrieve → generate → END
                 └(off_topic)─────→ reject ──────────────→ END
"""

from __future__ import annotations

from langgraph.checkpoint.base import BaseCheckpointSaver
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph

from app.graph.nodes import Nodes
from app.graph.state import ChatState
from app.llm import ChatLLM
from app.vectorstore import Retriever


def build_graph(
    llm: ChatLLM,
    retriever: Retriever,
    top_k: int,
    checkpointer: BaseCheckpointSaver | None = None,
):
    """그래프를 컴파일한다. 앱 기동 시 1회만 호출한다.

    checkpointer와 thread_id=session_id로 세션별 대화 이력을 유지한다(서버 재시작 시 초기화).
    """
    nodes = Nodes(llm=llm, retriever=retriever, top_k=top_k)

    graph = StateGraph(ChatState)
    graph.add_node("classify", nodes.classify)
    graph.add_node("retrieve", nodes.retrieve)
    graph.add_node("generate", nodes.generate)
    graph.add_node("reject", nodes.reject)

    graph.add_edge(START, "classify")
    graph.add_conditional_edges("classify", nodes.route, ["retrieve", "reject"])
    graph.add_edge("retrieve", "generate")
    graph.add_edge("generate", END)
    graph.add_edge("reject", END)

    return graph.compile(checkpointer=checkpointer or InMemorySaver())
