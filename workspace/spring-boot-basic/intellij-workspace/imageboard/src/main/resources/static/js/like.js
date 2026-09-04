// src/main/resources/static/js/like.js

async function toggleLike(btn) {
    const boardId = btn.dataset.boardId;

    try {
        btn.disabled = true;  // 중복 클릭 방지

        const result = await apiRequest(`/api/boards/${boardId}/likes`, {
            method: 'POST',
        });

        document.querySelector('#like-count').textContent = result.likeCount;
        btn.className = result.liked ? 'btn btn-danger' : 'btn btn-secondary';

    } catch (error) {
        if (error.message.includes('로그인')) {
            alert('로그인 후 좋아요를 누를 수 있습니다.');
            location.href = '/members/login';
        } else {
            alert('오류가 발생했습니다: ' + error.message);
        }
    } finally {
        btn.disabled = false;
    }
}
