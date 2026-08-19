/** @type {import('tailwindcss').Config} */
module.exports = {
  presets: [require('nativewind/preset')],
  content: [
    "./App.{js,ts,jsx,tsx}",
    "./mobile/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ink: '#191714',        // 주요 텍스트, 기본 버튼, 다크 패널
        paper: '#FFFDF9',      // 카드/입력 영역 밝은 배경
        cloud: '#F4F1EB',      // 아이콘 배경, 보조 영역
        line: '#D8D0C4',       // 카드/입력창/구분선
        gold: '#B47A26',       // 브랜드 강조, Eyebrow, 주요 아이콘
        champagne: '#E7C77D',  // 다크 배경 위 강조색, 여권 장식
        wine: '#7B2D35',       // VIP, 주의, 오류, 스탬프
        forest: '#23493F',     // 성공, 권한 확인, 개인정보 보호
        muted: '#736D65',      // 설명, 메타데이터, 보조 텍스트
        bg: '#F7F7F5',         // 앱 전체 배경
        inputBg: '#FAFAF8',
        segmentBg: '#ECEAE6',
        neutralPill: '#ECE8E1',
        goldPillBg: '#F4E6C4',
        successBg: '#DCE8E2',
        vipBg: '#F0DFE1',
        infoBg: '#EFF5F2',
        warnBg: '#FCF5F5',
        darkPanel2: '#2D2A26',
        darkDivider: '#4A4640',
        tabInactive: '#8A847C',
        stampBg: '#FAF3ED',
      },
      borderRadius: {
        DEFAULT: '8px',
        card: '8px',
        input: '8px',
        btn: '8px',
      },
      fontFamily: {
        sans: [
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          'Pretendard',
          '"Noto Sans KR"',
          'sans-serif'
        ]
      }
    },
  },
  plugins: [],
}
