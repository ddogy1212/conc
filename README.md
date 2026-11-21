# CONC · 갤럭시 1×1 집중 스위치 💜

홈 화면의 **1×1 CONC 위젯을 한 번 터치하면 30분 집중 모드**를 시작합니다.

## 기능
- 앱에서 차단할 앱 자유롭게 선택
- 집중 중 앱 내 취소 버튼 없음 · 30분 후 자동 종료
- 선택한 앱을 열면 차단 화면과 남은 시간 표시
- 갤러리 사진을 위젯 배경으로 설정
- 같은 사진으로 갤럭시 홈 화면의 **별도 앱 바로가기 아이콘** 만들기
- 전화·긴급 서비스·안드로이드 필수 앱 차단 제외

## 설치
1. GitHub Actions가 빌드에 성공하면 [`conc-debug.apk`](downloads/conc-debug.apk)를 선택해 설치할 수 있습니다. (이 파일이 아직 없으면 Actions → Build installable Android APK → Run workflow)
2. CONC 앱 실행 → 차단할 앱 선택
3. 앱 안에서 `접근성 설정 열기` → **CONC 앱 집중 제한** 허용 (처음 한 번만)
4. 홈 화면 길게 누르기 → 위젯 → CONC → **1×1** 배치
5. 그다음부터 위젯을 누르면 즉시 30분 집중 시작!

> 안드로이드에서 일반 앱이 설정 변경·권한 해제·앱 제거를 원천 차단할 수는 없습니다. 접근성 서비스를 본인이 끄면 차단도 멈춥니다. 긴급 통화나 기기 설정 기능을 막지 않습니다.

## 개발 / APK 빌드
`gradle :app:assembleDebug` (Gradle 8.9, JDK 17, Android SDK 35). GitHub Actions가 컴파일하고 결과를 `downloads/conc-debug.apk`로 자동 커밋합니다. 앱의 기본 설치 아이콘은 사진으로 동적 변경할 수 없지만, 앱에서 선택한 사진으로 별도 홈 화면 바로가기를 만들 수 있습니다.

## CI 확인
GitHub Actions PR build validates that the APK can be compiled.
