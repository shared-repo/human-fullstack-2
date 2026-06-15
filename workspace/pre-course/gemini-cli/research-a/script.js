document.addEventListener('DOMContentLoaded', () => {
    // 2026년 5월 29일 IT 뉴스 데이터
    const newsData = [
        {
            id: 1,
            title: "앤스로픽, 오픈AI 제치고 기업 가치 1위 등극",
            category: "AI",
            summary: "앤스로픽이 650억 달러 규모의 펀딩을 유치하며 기업 가치 9,650억 달러를 기록, 오픈AI를 제치고 세계에서 가장 가치 있는 민간 AI 기업이 되었습니다.",
            date: "2026.05.29"
        },
        {
            id: 2,
            title: "삼성전자, 업계 최초 12단 HBM4E 샘플 출하",
            category: "반도체",
            summary: "삼성전자가 16Gbps 속도와 향상된 에너지 효율을 갖춘 12단 HBM4E 샘플을 출하하며 차세대 AI 가속기 시장에서의 주도권 확보에 나섰습니다.",
            date: "2026.05.29"
        },
        {
            id: 3,
            title: "CISA, Nx Console 및 TanStack 관련 보안 취약점 경고",
            category: "보안",
            summary: "미국 사이버보안국(CISA)이 Nx Console과 TanStack에서 발견된 새로운 보안 취약점을 '알려진 악용된 취약점' 카탈로그에 추가하고 즉각적인 패치를 권고했습니다.",
            date: "2026.05.29"
        },
        {
            id: 4,
            title: "2026년, '에이전틱(Agentic)' AI의 시대로 전환",
            category: "트렌드",
            summary: "가트너와 IBM의 보고서에 따르면, 올해는 단순한 챗봇을 넘어 자율적인 AI 에이전트들이 협업하여 업무를 완수하는 '멀티에이전트 시스템'이 본격화되는 해입니다.",
            date: "2026.05.29"
        },
        {
            id: 5,
            title: "모질라, 앤스로픽 'Mythos' 모델 활용해 파이어폭스 버그 수정",
            category: "소프트웨어",
            summary: "모질라가 앤스로픽의 차세대 AI 모델 'Mythos'를 사용하여 파이어폭스 150 정식 출시 전 수백 개의 보안 취약점을 사전에 식별하고 패치하는 데 성공했습니다.",
            date: "2026.05.29"
        },
        {
            id: 6,
            title: "IBM, 양자 컴퓨터의 '실용적 유용성' 단계 진입 선언",
            category: "양자 컴퓨팅",
            summary: "IBM은 자사의 양자 컴퓨터가 재료 과학 시뮬레이션 분야에서 기존 고성능 컴퓨터(HPC)의 성능을 뛰어넘는 실용적 성과를 거두었다고 발표했습니다.",
            date: "2026.05.29"
        }
    ];

    const newsContainer = document.getElementById('news-container');
    const searchInput = document.getElementById('search-input');

    // 뉴스 카드 렌더링 함수
    function renderNews(newsList) {
        newsContainer.innerHTML = '';

        if (newsList.length === 0) {
            newsContainer.innerHTML = '<div class="no-results">검색 결과가 없습니다.</div>';
            return;
        }

        newsList.forEach(news => {
            const card = document.createElement('article');
            card.className = 'news-card';
            card.innerHTML = `
                <div class="card-content">
                    <span class="category">${news.category}</span>
                    <h2>${news.title}</h2>
                    <p class="summary">${news.summary}</p>
                </div>
                <div class="card-footer">
                    <span>${news.date}</span>
                </div>
            `;
            newsContainer.appendChild(card);
        });
    }

    // 초기 렌더링
    renderNews(newsData);

    // 검색 필터링 이벤트
    searchInput.addEventListener('input', (e) => {
        const searchTerm = e.target.value.toLowerCase();
        const filteredNews = newsData.filter(news => 
            news.title.toLowerCase().includes(searchTerm) || 
            news.category.toLowerCase().includes(searchTerm) ||
            news.summary.toLowerCase().includes(searchTerm)
        );
        renderNews(filteredNews);
    });
});
