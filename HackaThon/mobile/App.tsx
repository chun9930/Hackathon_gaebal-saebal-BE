import "../global.css";
import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import {
  Alert,
  FlatList,
  Image,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  useWindowDimensions,
  View,
} from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { NavigationContainer, useNavigation } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";
import { StatusBar } from "expo-status-bar";
import { CameraView, useCameraPermissions } from "expo-camera";
import { useFonts } from "expo-font";
import * as ImagePicker from "expo-image-picker";
import QRCode from "react-native-qrcode-svg";
import Svg, { Rect } from "react-native-svg";
import Animated, {
  Easing,
  FadeIn,
  FadeInUp,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from "react-native-reanimated";
import {
  Award,
  BookOpen,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Compass,
  FileText,
  Heart,
  Home,
  LogOut,
  MapPinned,
  MapPin,
  QrCode,
  ScanLine,
  Search,
  Smartphone,
  Stamp,
  UserRound,
  X,
} from "lucide-react-native";
import { MOCK_CUSTOMERS } from "../src/mock/customers";
import { RECOMMENDABLE_PRODUCTS } from "../src/mock/products";
import { MOCK_BRIEFS } from "../src/mock/briefs";
import type { Customer, JourneyStamp, ProductRecommendation, UserRole } from "../src/types";
import type { CustomerProfileResponse, CustomerSearchItem } from "../src/api/contracts";
import { colors as c } from "./theme";
import { authApi, caApi, customerApi, employeeApi, getApiErrorCode, getApiErrorMessage, productApi, resolveApiUrl, setAccessToken } from "./api";
import { getCustomerSearchErrorMessage, normalizeCustomerSearchKeyword } from "./customer-search";

type AuthScreen = "login" | "signup";
type StoreName = keyof typeof STORE_STAMP_IMAGES;
type ConsultationDraft = Omit<import("../src/types").ConsultationNote, "id" | "createdAt">;
type AppState = {
  role: UserRole;
  setRole: (v: UserRole) => void;
  isLoggedIn: boolean;
  caCustomersLoading: boolean;
  logout: () => void;
  authScreen: AuthScreen;
  setAuthScreen: (v: AuthScreen) => void;
  customers: Customer[];
  customer: Customer;
  products: ProductRecommendation[];
  select: (id: string) => void;
  toggleProduct: (id: string) => void;
  currentStore: StoreName;
  setCurrentStore: (store: StoreName) => void;
  currentCaName: string;
  setCurrentCaName: (name: string) => void;
  addStamp: (id: string, type: JourneyStamp["type"]) => void;
  addConsultation: (id: string, note: ConsultationDraft, visitRecordId: number) => void;
  updateConsultation: (customerId: string, noteId: string, note: ConsultationDraft) => void;
  deleteConsultation: (customerId: string, noteId: string) => void;
  updateAvatar: (uri: string) => void;
  syncCustomerProfile: (profile: CustomerProfileResponse) => void;
  syncCustomerStamps: (customerId: string, stamps: JourneyStamp[]) => void;
  syncCustomerSearchResults: (items: CustomerSearchItem[]) => Customer[];
};
const Ctx = createContext<AppState | null>(null);
const useApp = () => useContext(Ctx)!;
const storageKey = "mcm-mobile-customers";
// 목업도 실 API와 동일하게 customerNo와 분리된 QR 토큰만 화면에 노출한다.
const INITIAL_CUSTOMERS: Customer[] = MOCK_CUSTOMERS.map((customer) => ({
  ...customer,
  qrToken: customer.qrToken ?? `demo-qr-${customer.id}`,
}));
const GENERAL_MEMBERSHIP_TIER = INITIAL_CUSTOMERS.find((customer) => customer.membershipTier !== "VIP")?.membershipTier ?? "VIP";

function toFrontendCustomer(profile: CustomerProfileResponse): Customer {
  return {
    id: String(profile.customerId),
    name: profile.name,
    customerNo: profile.customerNo,
    qrToken: profile.qrToken,
    phoneLast4: profile.phoneNumber?.slice(-4) ?? "0000",
    membershipTier: profile.membershipGrade === "VIP" ? "VIP" : GENERAL_MEMBERSHIP_TIER,
    points: 0,
    preferredStyle: profile.stylePreferences ? [profile.stylePreferences] : [],
    purchasePurpose: "",
    cautionNotes: undefined,
    visitCount: profile.visitCount ?? 0,
    joinedAt: profile.joinedAt?.slice(0, 10) ?? new Date().toISOString().slice(0, 10),
    avatarUrl: profile.profileImageUrl,
    stamps: [],
    purchases: [],
    careRecords: [],
    consultations: [],
    savedProductIds: [],
  };
}
function toFrontendSearchCustomer(item: CustomerSearchItem): Customer {
  return {
    id: String(item.customerId),
    name: item.name,
    customerNo: item.customerNo,
    qrToken: "",
    phoneLast4: item.phoneNumber?.slice(-4) ?? "0000",
    membershipTier: item.membershipGrade === "VIP" ? "VIP" : GENERAL_MEMBERSHIP_TIER,
    points: 0,
    preferredStyle: [],
    purchasePurpose: "",
    cautionNotes: undefined,
    visitCount: 0,
    joinedAt: item.joinedAt?.slice(0, 10) ?? new Date().toISOString().slice(0, 10),
    avatarUrl: item.profileImageUrl,
    stamps: [],
    purchases: [],
    careRecords: [],
    consultations: [],
    savedProductIds: [],
  };
}
function toFrontendJourneyStamp(stamp: import("../src/api/contracts").StampResponse): JourneyStamp {
  return {
    id: String(stamp.stampId),
    storeName: normalizeStoreName(stamp.storeName),
    type: stamp.stampType === "purchase" || stamp.stampType === "care" || stamp.stampType === "invite" ? stamp.stampType : "visit",
    issuedAt: stamp.issuedAt,
    issuedByCA: stamp.issuedByCaName,
    imageUrl: resolveApiUrl(stamp.stampImageUrl),
  };
}
const BRAND_LOGO = require("../logo.png");
// 화면 헤더용: 원본 PNG에 들어 있던 큰 투명 테두리를 제거한 버전이다.
// 따라서 로고의 보이는 왼쪽 끝을 본문 시작선에 정확히 맞출 수 있다.
const BRAND_LOGO_TIGHT = require("../logo-tight.png");
const RECOMMEND_ICON = require("../recommend.png");

// 스타일을 별도로 주지 않은 안내 문구도 Pretendard를 사용한다.
// 입력칸은 Android 한글 조합을 안정적으로 유지하기 위해 각 화면의 style로만 글꼴을 지정한다.
type FontDefaultComponent = { defaultProps?: { style?: unknown } };
(Text as unknown as FontDefaultComponent).defaultProps = { style: { fontFamily: "Pretendard" } };

// 국내 공식 지점용 여권 도장. 실제 발급 날짜는 고객 데이터에서 별도로 표시한다.
const STORE_STAMP_IMAGES: Record<string, number> = {
  "MCM 하우스 플래그십스토어": require("../stores/journey-stamp-seoul-haus-flagship.png"),
  "MCM 롯데백화점 잠실점": require("../stores/journey-stamp-seoul-lotte-jamsil.png"),
  "MCM 롯데백화점 본점": require("../stores/journey-stamp-seoul-lotte-main.png"),
  "MCM 신라면세점 서울점": require("../stores/journey-stamp-seoul-shilla-duty-free.png"),
  "MCM 신세계면세점 명동점": require("../stores/journey-stamp-seoul-shinsegae-duty-free-main.png"),
  "MCM 현대면세점 무역센터점": require("../stores/journey-stamp-seoul-hyundai-duty-free-trade-center.png"),
  "MCM 롯데면세점 월드타워점": require("../stores/journey-stamp-seoul-lotte-world-tower-duty-free.png"),
  "MCM 롯데면세점 본점": require("../stores/journey-stamp-seoul-lotte-duty-free-main.png"),
  "MCM 파주 프리미엄 아울렛": require("../stores/journey-stamp-paju-premium-outlet.png"),
  "MCM 대구 롯데백화점": require("../stores/journey-stamp-daegu-lotte.png"),
  "MCM 부산 롯데면세점": require("../stores/journey-stamp-busan-lotte-duty-free.png"),
  "MCM 인천 T1 현대면세점": require("../stores/journey-stamp-incheon-t1-hyundai-duty-free.png"),
  "MCM 제주 신라면세점": require("../stores/journey-stamp-jeju-shilla-duty-free.png"),
  "MCM 제주 롯데면세점": require("../stores/journey-stamp-jeju-lotte-duty-free.png"),
};
// 이전 데모 데이터와 새 지점명 사이의 연결표. 이미 저장된 고객 여정도 지점별 도장을 잃지 않는다.
const LEGACY_STORE_NAMES: Record<string, StoreName> = {
  "MCM HAUS": "MCM 하우스 플래그십스토어",
  "청담 플래그십 스토어": "MCM 하우스 플래그십스토어",
  "MCM 청담 플래그십": "MCM 하우스 플래그십스토어",
  "신세계 백화점 강남점": "MCM 신세계면세점 명동점",
  "안양 롯데 백화점": "MCM 롯데백화점 잠실점",
  "MCM 도쿄 긴자점": "MCM 롯데백화점 본점",
  "MCM 싱가포르 마리나베이": "MCM 제주 롯데면세점",
};
const normalizeStoreName = (storeName: string): string =>
  LEGACY_STORE_NAMES[storeName] ?? storeName;
const getStampAsset = (storeName: string) =>
  STORE_STAMP_IMAGES[storeName] ?? STORE_STAMP_IMAGES[LEGACY_STORE_NAMES[storeName]];
const formatStoreName = (storeName: string) =>
  storeName === "MCM 하우스 플래그십스토어"
    ? "MCM 하우스\n플래그십스토어"
    : storeName.replace(" 롯데백화점 ", " 롯데백화점\n").replace(" 면세점 ", " 면세점\n");
const isBackendCustomerId = (value: string) => /^\d+$/.test(value);

// 가로 스크롤 행의 실제 렌더 너비를 측정해, 카드가 화면 밖으로 잘리지 않도록
// 카드 폭을 그 너비에 맞춰 계산할 때 쓴다(도장 크기를 임의로 줄이지 않기 위함).
function useMeasuredWidth() {
  const [width, setWidth] = useState(0);
  const onLayout = (e: { nativeEvent: { layout: { width: number } } }) =>
    setWidth(e.nativeEvent.layout.width);
  return [width, onLayout] as const;
}

function Provider({ children }: { children: React.ReactNode }) {
  const [role, setRoleState] = useState<UserRole>("customer");
  const [isLoggedIn, setLoggedIn] = useState(false);
  const [caCustomersLoading, setCaCustomersLoading] = useState(false);
  const [authScreen, setAuthScreen] = useState<AuthScreen>("login");
  const setRole = (nextRole: UserRole) => {
    setRoleState(nextRole);
    if (nextRole === "ca") {
      setCaCustomersLoading(false);
      setCustomers([]);
      select("");
    } else {
      setCaCustomersLoading(false);
    }
    setLoggedIn(true);
  };
  const logout = () => {
    setAccessToken(null);
    setLoggedIn(false);
    setRoleState("customer");
    setAuthScreen("login");
  };
  const [customers, setCustomers] = useState<Customer[]>(INITIAL_CUSTOMERS);
  const [products, setProducts] = useState<ProductRecommendation[]>(RECOMMENDABLE_PRODUCTS);
  const [selected, select] = useState("cust-01");
  const [currentStore, setCurrentStore] = useState<StoreName>(
    "MCM 하우스 플래그십스토어",
  );
  const [currentCaName, setCurrentCaName] = useState("이지원");
  useEffect(() => {
    AsyncStorage.getItem(storageKey)
      .then((v) => {
        if (!v) return;
        const savedCustomers = JSON.parse(v) as Customer[];
        setCustomers(
          savedCustomers.map((customer) => {
            const stamps = customer.stamps.map((stamp) => ({
              ...stamp,
              storeName: LEGACY_STORE_NAMES[stamp.storeName] ?? stamp.storeName,
            }));
            const storeFor = (type: JourneyStamp["type"], date: string) =>
              stamps.find((stamp) => stamp.type === type && stamp.issuedAt.slice(0, 10) === date)?.storeName ??
              stamps.find((stamp) => stamp.type === type)?.storeName ??
              stamps[0]?.storeName ??
              "MCM 하우스 플래그십스토어";
            return {
              ...customer,
              qrToken: customer.qrToken ?? `demo-qr-${customer.id}`,
              stamps,
              purchases: customer.purchases.map((purchase) => ({ ...purchase, storeName: purchase.storeName ?? storeFor("purchase", purchase.purchasedAt) })),
              careRecords: customer.careRecords.map((record) => ({ ...record, storeName: record.storeName ?? storeFor("care", record.date) })),
            };
          }),
        );
      })
      .catch(() => undefined);
  }, []);
  useEffect(() => {
    if (role !== "customer") return;
    AsyncStorage.setItem(storageKey, JSON.stringify(customers));
  }, [customers, role]);
  useEffect(() => {
    if (!isLoggedIn) return;
    productApi.list()
      .then((items) => setProducts(items.filter((product) => product.recommendable)))
      .catch(() => undefined);
  }, [isLoggedIn]);
  const customer = customers.find((x) => x.id === selected) ?? customers[0] ?? INITIAL_CUSTOMERS[0];
  const syncCustomerProfile = (profile: CustomerProfileResponse) => {
    const nextCustomer = toFrontendCustomer(profile);
    setCustomers((all) => {
      const existing = all.find((item) => item.id === nextCustomer.id);
      if (!existing) return [nextCustomer, ...all];
      return all.map((item) =>
        item.id !== nextCustomer.id
          ? item
          : {
              ...existing,
              ...nextCustomer,
              stamps: existing.stamps,
              purchases: existing.purchases,
              careRecords: existing.careRecords,
              consultations: existing.consultations,
              savedProductIds: existing.savedProductIds,
            },
      );
    });
    select(nextCustomer.id);
  };
  const syncCustomerStamps = (customerId: string, stamps: JourneyStamp[]) => {
    setCustomers((all) =>
      all.map((customer) =>
        customer.id !== customerId
          ? customer
          : {
              ...customer,
              stamps: [...stamps].sort((a, b) => b.issuedAt.localeCompare(a.issuedAt)),
              visitCount: Math.max(customer.visitCount, stamps.filter((stamp) => stamp.type === "visit").length),
            },
      ),
    );
  };
  const syncCustomerSearchResults = (items: CustomerSearchItem[]) => {
    const nextCustomers = items.map(toFrontendSearchCustomer);
    setCustomers(nextCustomers);
    select(nextCustomers[0]?.id ?? "");
    return nextCustomers;
  };
  const value = useMemo(
    () => ({
      role,
      setRole,
      isLoggedIn,
      caCustomersLoading,
      logout,
      authScreen,
      setAuthScreen,
      customers,
      customer,
      products,
      select,
      currentStore,
      setCurrentStore,
      currentCaName,
      setCurrentCaName,
      toggleProduct: (id: string) =>
        setCustomers((all) =>
          all.map((x) =>
            x.id !== selected
              ? x
              : {
                  ...x,
                  savedProductIds: x.savedProductIds.includes(id)
                    ? x.savedProductIds.filter((v) => v !== id)
                    : [...x.savedProductIds, id],
                },
          ),
        ),
      addStamp: (id: string, type: JourneyStamp["type"]) =>
        setCustomers((all) =>
          all.map((x) =>
            x.id !== id
              ? x
              : {
                  ...x,
                  visitCount: x.visitCount + 1,
                  stamps: [
                    {
                      id: `stamp-${Date.now()}`,
                      type,
                      storeName: currentStore,
                      issuedAt: new Date().toISOString(),
                      issuedByCA: `${currentCaName} CA`,
                    },
                    ...x.stamps,
                  ],
                },
          ),
        ),
      addConsultation: (id: string, note: ConsultationDraft, visitRecordId: number) =>
        setCustomers((all) =>
          all.map((x) =>
            x.id !== id
              ? x
              : {
                  ...x,
                  consultations: [
                    { ...note, id: String(visitRecordId), createdAt: new Date().toISOString() },
                    ...x.consultations,
                  ],
                },
          ),
        ),
      updateConsultation: (customerId: string, noteId: string, note: ConsultationDraft) =>
        setCustomers((all) =>
          all.map((customer) =>
            customer.id !== customerId
              ? customer
              : {
                  ...customer,
                  consultations: customer.consultations.map((item) =>
                    item.id === noteId ? { ...item, ...note } : item,
                  ),
                },
          ),
        ),
      deleteConsultation: (customerId: string, noteId: string) =>
        setCustomers((all) =>
          all.map((customer) =>
            customer.id !== customerId
              ? customer
              : { ...customer, consultations: customer.consultations.filter((item) => item.id !== noteId) },
          ),
        ),
      updateAvatar: (uri: string) =>
        setCustomers((all) =>
          all.map((x) => (x.id === selected ? { ...x, avatarUrl: uri } : x)),
        ),
      syncCustomerProfile,
      syncCustomerStamps,
      syncCustomerSearchResults,
    }),
    [role, isLoggedIn, caCustomersLoading, authScreen, customers, selected, currentStore, currentCaName, products],
  );
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

// 사운드웨이브 아이콘: lucide 아이콘과 동일하게 color/size props로 자리에 그대로 끼워 넣을 수 있다.
function SoundWaveIcon({ color = "#000", size = 24 }: { color?: string; size?: number }) {
  const barHeights = [4, 8, 12, 16, 20, 16, 12, 8, 4];
  const barWidth = 1.6;
  const gap = 1;
  const startX = 0.8;
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24">
      {barHeights.map((h, i) => (
        <Rect
          key={i}
          x={startX + i * (barWidth + gap)}
          y={12 - h / 2}
          width={barWidth}
          height={h}
          rx={barWidth / 2}
          fill={color}
        />
      ))}
    </Svg>
  );
}
function Pill({
  children,
  tone = "gold",
}: {
  children: React.ReactNode;
  tone?: "gold" | "wine" | "forest";
}) {
  const bg =
    tone === "wine" ? "#F0DFE1" : tone === "forest" ? "#DCE8E2" : "#F4E6C4";
  const fg = tone === "wine" ? c.wine : tone === "forest" ? c.forest : c.gold;
  return (
    <View style={[s.pill, { backgroundColor: bg }]}>
      <Text style={[s.pillText, { color: fg }]}>{children}</Text>
    </View>
  );
}
const AnimatedPressable = Animated.createAnimatedComponent(Pressable);
// 탭 가능한 타일 전반에서 재사용하는, 아주 살짝 눌렸다가 천천히 돌아오는 절제된 터치 스케일.
function usePressScale(target = 0.97) {
  const scale = useSharedValue(1);
  const style = useAnimatedStyle(() => ({ transform: [{ scale: scale.value }] }));
  const onPressIn = () => {
    scale.value = withTiming(target, { duration: 140, easing: Easing.out(Easing.quad) });
  };
  const onPressOut = () => {
    scale.value = withTiming(1, { duration: 260, easing: Easing.out(Easing.quad) });
  };
  return { style, onPressIn, onPressOut };
}
// 명품 앱 특유의 절제된 터치감: 빠르게 튀지 않고 아주 살짝 눌렸다가 천천히 돌아온다.
function Button({
  children,
  onPress,
  secondary = false,
  icon,
  disabled = false,
}: {
  children: React.ReactNode;
  onPress: () => void;
  secondary?: boolean;
  icon?: React.ReactNode;
  disabled?: boolean;
}) {
  const scale = useSharedValue(1);
  const iconShift = useSharedValue(0);
  const pressStyle = useAnimatedStyle(() => ({ transform: [{ scale: scale.value }] }));
  const iconStyle = useAnimatedStyle(() => ({ transform: [{ translateX: iconShift.value }] }));
  return (
    <AnimatedPressable
      onPress={onPress}
      disabled={disabled}
      onPressIn={() => {
        scale.value = withTiming(0.97, { duration: 140, easing: Easing.out(Easing.quad) });
        iconShift.value = withTiming(3, { duration: 220, easing: Easing.out(Easing.quad) });
      }}
      onPressOut={() => {
        scale.value = withTiming(1, { duration: 260, easing: Easing.out(Easing.quad) });
        iconShift.value = withTiming(0, { duration: 260, easing: Easing.out(Easing.quad) });
      }}
      style={[s.button, secondary && s.buttonSecondary, disabled && { opacity: 0.55 }, pressStyle]}
    >
      <View style={s.buttonContent}>
        {icon && <Animated.View style={iconStyle}>{icon}</Animated.View>}
        {
          <Text style={[s.buttonText, secondary && s.buttonTextSecondary]}>
            {children}
          </Text>
        }
      </View>
    </AnimatedPressable>
  );
}
// 카드가 화면에 들어올 때 아주 미세하게 떠오르며 나타난다(deley로 목록에 순차적인 리듬을 줄 수 있다).
function Card({
  children,
  dark = false,
  delay = 0,
  style,
}: {
  children: React.ReactNode;
  dark?: boolean;
  delay?: number;
  style?: any;
}) {
  return (
    <Animated.View
      entering={FadeInUp.duration(420).delay(delay).easing(Easing.out(Easing.cubic))}
      style={[s.card, dark && s.darkCard, style]}
    >
      {children}
    </Animated.View>
  );
}
function Header({
  title,
  back = false,
  caMode = false,
  logoOnly = false,
}: {
  title: string;
  back?: boolean;
  caMode?: boolean;
  logoOnly?: boolean;
}) {
  const n = useNavigation<any>();
  const { logout, currentStore, currentCaName } = useApp();
  const { isTablet } = useResponsive();
  return (
    <View style={[s.header, logoOnly && !isTablet && s.headerHome]}>
      <Pressable
        hitSlop={12}
        onPress={() => (back ? n.goBack() : undefined)}
        style={[s.headerMark, !back && s.headerLogoMark, logoOnly && !isTablet && s.headerHomeMark]}
      >
        {back ? (
          <ChevronLeft color={c.champagne} size={24} strokeWidth={2.6} />
        ) : (
          <Image source={logoOnly || !caMode ? BRAND_LOGO_TIGHT : BRAND_LOGO} style={[s.headerLogo, caMode && s.caHeaderLogo, caMode && !isTablet && s.caHeaderLogoPhone, logoOnly && !isTablet && s.headerHomeLogo]} resizeMode="contain" />
        )}
      </Pressable>
      {caMode ? (
        <>
          {back && <View style={{ flex: 1 }}><Text style={s.headerTitle}>{title}</Text></View>}
          <View style={s.caHeaderIdentity}>
            <View><Text style={s.caHeaderName}>CA {currentCaName}</Text><Text style={s.caHeaderStore}>{currentStore.replace("MCM ", "")}</Text></View>
            {!back && <Pressable accessibilityLabel="로그아웃" onPress={logout} style={s.caHeaderLogout}><LogOut color={c.ink} size={14} /></Pressable>}
          </View>
        </>
      ) : !logoOnly ? (
        <View style={{ flex: 1 }}>
          <Text style={s.headerKicker}>MCM PRIVATE CIRCLE</Text>
          <Text style={s.headerTitle}>{title}</Text>
        </View>
      ) : null}
    </View>
  );
}
type ScreenPreset = "compact" | "content" | "wide";
function useResponsive() {
  const { width } = useWindowDimensions();
  return {
    width,
    isPhone: width < 768,
    isTablet: width >= 768,
    isWide: width >= 1024,
    horizontalPadding: width >= 1024 ? 20 : width >= 640 ? 24 : 16,
  };
}
function Screen({
  children,
  title,
  back = false,
  preset = "content",
  caHeader = false,
  homeHeader = false,
}: {
  children: React.ReactNode;
  title?: string;
  back?: boolean;
  preset?: ScreenPreset;
  caHeader?: boolean;
  homeHeader?: boolean;
}) {
  const { horizontalPadding } = useResponsive();
  const maxWidth = preset === "compact" ? 560 : preset === "wide" ? 1180 : 820;
  return (
    <SafeAreaView style={s.safe}>
      <StatusBar style="dark" />
      {(title || caHeader || homeHeader) && <Header title={title ?? ""} back={back} caMode={caHeader} logoOnly={homeHeader} />}
      <ScrollView
        contentContainerStyle={s.scrollOuter}
        keyboardShouldPersistTaps="handled"
      >
        <Animated.View
          entering={FadeIn.duration(360)}
          style={[s.scroll, { maxWidth, paddingHorizontal: horizontalPadding }]}
        >
          {children}
        </Animated.View>
      </ScrollView>
    </SafeAreaView>
  );
}

function Login() {
  const { setRole, setAuthScreen, setCurrentStore, setCurrentCaName, syncCustomerProfile, syncCustomerStamps } = useApp();
  const { isTablet, horizontalPadding } = useResponsive();
  const [role, choose] = useState<UserRole>("customer");
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const enter = async () => {
    if (submitting) return;
    if (!identifier.trim() || !password) {
      Alert.alert("확인 필요", "로그인 아이디와 비밀번호를 입력해 주세요.");
      return;
    }
    setSubmitting(true);
    try {
      if (role === "customer") {
        await authApi.customerLogin(identifier.trim(), password);
        const profile = await customerApi.me();
        syncCustomerProfile(profile);
        const stamps = await customerApi.stamps();
        syncCustomerStamps(
          String(profile.customerId),
          stamps.items.map(toFrontendJourneyStamp),
        );
      } else {
        await authApi.employeeLogin(identifier.trim(), password);
        const profile = await employeeApi.me();
        setCurrentStore(normalizeStoreName(profile.storeName) as StoreName);
        setCurrentCaName(profile.name);
      }
      setRole(role);
    } catch {
      Alert.alert("로그인 실패", "아이디 또는 비밀번호를 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  };
  const isCustomer = role === "customer";
  return (
    <SafeAreaView style={[s.safe, s.login, isTablet && s.loginTablet]}>
      <StatusBar style="light" />
      <View style={[s.loginDark, isTablet && s.loginDarkTablet]}>
        <View style={isTablet ? [s.loginInner, s.loginInnerTablet] : undefined}>
          <Image source={BRAND_LOGO_TIGHT} style={[s.loginLogo, isTablet && s.loginLogoTablet]} resizeMode="contain" />
          <View style={[s.loginHeroSpacer, isTablet && s.loginHeroSpacerTablet]} />
          <Pill>JOURNEY PASSPORT</Pill>
          <Text
            numberOfLines={isTablet ? undefined : 1}
            adjustsFontSizeToFit={!isTablet}
            minimumFontScale={0.58}
            style={[s.loginHeadline, isTablet && s.loginHeadlineTablet]}
          >
            {isTablet ? "고객의 모든 여정을\n더 특별하게 기억합니다" : "고객의 모든 여정을 더 특별하게 기억합니다"}
          </Text>
          <Text
            numberOfLines={isTablet ? undefined : 1}
            adjustsFontSizeToFit={!isTablet}
            minimumFontScale={0.5}
            style={[s.darkBody, s.loginHeroBody]}
          >
            {isTablet ? "방문·상담·구매·케어 이력을\n하나의 프라이빗 여권에 담습니다." : "방문·상담·구매·케어 이력을 하나의 프라이빗 여권에 담습니다."}
          </Text>
        </View>
      </View>
      <ScrollView
        contentContainerStyle={[
          s.loginForm,
          isTablet && s.loginFormTablet,
          {
            paddingHorizontal: isTablet ? Math.max(horizontalPadding, 48) : 24,
          },
        ]}
        keyboardShouldPersistTaps="handled"
      >
        <View style={isTablet ? s.loginFormInner : undefined}>
          <View style={s.authTopRow}>
            <View>
              <Text style={[s.kicker, s.loginKicker]}>
                {isCustomer ? "WELCOME BACK" : "CA WORKSTATION"}
              </Text>
              <Text style={[s.pageTitle, s.loginTitle]}>
                {isCustomer ? "로그인" : "CA 로그인"}
              </Text>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="CA 로그인 전환"
              onPress={() => {
                choose(isCustomer ? "ca" : "customer");
                setIdentifier("");
                setPassword("");
              }}
              style={[s.roleSwitch, !isCustomer && s.roleSwitchActive]}
            >
              <Smartphone size={20} color={!isCustomer ? c.paper : c.ink} />
              <Text
                style={[
                  s.roleSwitchText,
                  !isCustomer && s.roleSwitchTextActive,
                ]}
              >
                {isCustomer ? "CA" : "고객"}
              </Text>
            </Pressable>
          </View>
          <View style={s.loginIntro}>
            <Text style={s.body}>
              {isCustomer
                ? "MCM Private Circle 고객 계정으로 로그인하세요."
                : "매장 담당자 전용 로그인입니다."}
            </Text>
          </View>
          <View style={s.authField}>
            <Text style={s.label}>{isCustomer ? "로그인 아이디" : "담당 CA 사번"}</Text>
            <TextInput style={s.textInput} value={identifier} onChangeText={setIdentifier} placeholder={isCustomer ? "가입한 아이디를 입력하세요" : "CA-1092"} placeholderTextColor={c.muted} autoCapitalize="none" />
          </View>
          <View style={[s.authField, s.passwordField]}>
            <Text style={s.label}>비밀번호</Text>
            <TextInput style={s.textInput} value={password} onChangeText={setPassword} placeholder="비밀번호를 입력하세요" placeholderTextColor={c.muted} secureTextEntry />
          </View>
          <View style={s.loginButtonWrap}>
            <Button onPress={enter} icon={<ChevronRight color={c.paper} size={24} />}>
              {isCustomer ? "로그인하고 여권 보기" : "CA Workstation 시작"}
            </Button>
          </View>
          {isCustomer && (
            <>
              <Pressable
                accessibilityRole="button"
                onPress={() => setAuthScreen("signup")}
                style={s.signupLink}
              >
                <Text style={s.signupText}>아직 계정이 없으신가요? </Text>
                <Text style={s.signupLinkText}>회원가입</Text>
              </Pressable>
              <Text style={s.demo}>가입한 계정으로 로그인해 주세요.</Text>
            </>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
function SignUp() {
  const { setAuthScreen, setRole, syncCustomerProfile, syncCustomerStamps } = useApp();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const submit = async () => {
    if (submitting) return;
    if (!name.trim() || !email.trim() || !phone.trim() || !password.trim()) {
      Alert.alert("확인 필요", "이름, 아이디, 휴대폰 번호, 비밀번호를 모두 입력해 주세요.");
      return;
    }
    const digitsOnlyPhone = phone.replace(/\D/g, "");
    if (!/^01[0-9]{8,9}$/.test(digitsOnlyPhone)) {
      Alert.alert("확인 필요", "휴대폰 번호 형식을 확인해 주세요. (예: 01012345678)");
      return;
    }
    setSubmitting(true);
    try {
      await authApi.customerSignup({
        loginId: email.trim(),
        password,
        name: name.trim(),
        phoneNumber: digitsOnlyPhone,
      });
      const profile = await customerApi.me();
      syncCustomerProfile(profile);
      const stamps = await customerApi.stamps();
      syncCustomerStamps(
        String(profile.customerId),
        stamps.items.map(toFrontendJourneyStamp),
      );
      setRole("customer");
    } catch {
      Alert.alert("회원가입 실패", "입력하신 정보를 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  };
  return (
    <SafeAreaView style={s.safe}>
      <StatusBar style="dark" />
      <ScrollView
        contentContainerStyle={s.signupOuter}
        keyboardShouldPersistTaps="handled"
      >
        <View style={s.signupInner}>
          <Pressable
            accessibilityRole="button"
            onPress={() => setAuthScreen("login")}
            style={s.backText}
          >
            <Text style={s.link}><Text style={s.backChevron}>‹</Text> 로그인으로 돌아가기</Text>
          </Pressable>
          <Text style={s.signupKicker}>PRIVATE CIRCLE MEMBERSHIP</Text>
          <Text style={s.pageTitle}>회원가입</Text>
          <Text style={s.body}>
            MCM과의 특별한 여정을 시작하기 위한{"\n"}기본 정보를 입력해 주세요.
          </Text>
          <Card>
            <Text style={s.label}>이름</Text>
            <TextInput
              style={s.textInput}
              value={name}
              onChangeText={setName}
              placeholder="이름을 입력하세요"
              placeholderTextColor={c.muted}
            />
            <Text style={s.label}>아이디</Text>
            <TextInput
              style={s.textInput}
              value={email}
              onChangeText={setEmail}
              placeholder="가입할 아이디를 입력하세요"
              placeholderTextColor={c.muted}
              autoCapitalize="none"
            />
            <Text style={s.label}>휴대폰 번호</Text>
            <TextInput
              style={s.textInput}
              value={phone}
              onChangeText={setPhone}
              placeholder="01012345678"
              placeholderTextColor={c.muted}
              keyboardType="phone-pad"
            />
            <Text style={s.label}>비밀번호</Text>
            <TextInput
              style={s.textInput}
              value={password}
              onChangeText={setPassword}
              placeholder="8자 이상 입력하세요"
              placeholderTextColor={c.muted}
              secureTextEntry
            />
            <Text style={s.caption}>
              가입 후 MCM과의 여정을 Journey Passport에서 관리할 수 있습니다.
            </Text>
          </Card>
          <Button
            onPress={submit}
          >
            회원가입 완료
          </Button>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
function Splash({ onComplete }: { onComplete: () => void }) {
  useEffect(() => {
    const timer = setTimeout(onComplete, 1800);
    return () => clearTimeout(timer);
  }, [onComplete]);
  return (
    <SafeAreaView style={s.splash}>
      <StatusBar style="light" />
      <Pressable style={s.splashPress} onPress={onComplete}>
        <Image source={BRAND_LOGO} style={s.splashLogo} resizeMode="contain" />
      </Pressable>
    </SafeAreaView>
  );
}

function CustomerHome() {
  const { customer, products } = useApp();
  const n = useNavigation<any>();
  const [qr, setQr] = useState(false);
  const [stampRowWidth, onStampRowLayout] = useMeasuredWidth();
  const recentStamps = customer.stamps.slice(0, 3);
  const stampGap = 10;
  const stampCardSize = stampRowWidth
    ? Math.min(132, (stampRowWidth - stampGap * (recentStamps.length - 1)) / Math.max(recentStamps.length, 1))
    : 124;
  return (
    <Screen homeHeader>
      <Text style={s.kicker}>MCM JOURNEY PASSPORT</Text>
      <Text style={s.homeGreeting}>안녕하세요, {customer.name} 님</Text>
      <Text style={s.body}>국내 MCM 매장에서 기록한 나만의 여정입니다.</Text>
      <Card dark>
        <Text style={s.darkKicker}>OFFICIAL DIGITAL PASSPORT</Text>
        <Text style={s.passportName}>{customer.name}</Text>
        <Text style={s.darkBody}>{customer.customerNo}</Text>
        <View style={[s.stats, s.homeStats]}>
          <Stat
            dark
            label="누적 방문 스탬프"
            value={`${customer.stamps.length}개 도장`}
          />
          <View style={s.homeStatsSpacer} />
          <View style={s.homeJoinedStat}>
            <Stat dark label="가입일" value={customer.joinedAt} />
          </View>
        </View>
        <View style={s.row}>
          <View style={{ flex: 1 }}>
            <Button
              secondary
              onPress={() => setQr(true)}
              icon={<QrCode size={18} color={c.ink} />}
            >
              식별 QR코드
            </Button>
          </View>
          <View style={{ width: 12 }} />
          <View style={{ flex: 1 }}>
            <Button
              secondary
              onPress={() => n.navigate("Passport")}
              icon={<BookOpen size={18} color={c.ink} />}
            >
              여권 상세
            </Button>
          </View>
        </View>
      </Card>
      <SectionTitle
        kicker="PASSPORT STAMP"
        title="최근 방문 여정 도장"
        action={() => n.navigate("Journey")}
      />
      <FlatList
        horizontal
        onLayout={onStampRowLayout}
        data={recentStamps}
        keyExtractor={(x: JourneyStamp) => x.id}
        showsHorizontalScrollIndicator={false}
        renderItem={({ item }: { item: JourneyStamp }) => <StampCard item={item} compact size={stampCardSize} />}
        contentContainerStyle={{ gap: stampGap }}
      />
      <SectionTitle kicker="PRIVATE RECOMMEND" title="고객 맞춤 추천 제품" action={() => n.navigate("Recommendations")} />
      <ProductList products={products.slice(0, 3)} />
      <View style={s.homeActionList}>
        <Pressable onPress={() => n.navigate("Passport")} style={s.homeActionDark}>
          <BookOpen color={c.paper} size={24} />
          <Text style={s.homeActionDarkText}>Journey Passport</Text>
          <ChevronRight color={c.paper} size={24} />
        </Pressable>
        <Pressable onPress={() => n.navigate("Saved")} style={s.homeActionLight}>
          <Heart color={c.gold} size={24} />
          <Text style={s.homeActionLightText}>관심 저장 제품</Text>
          <ChevronRight color={c.gold} size={24} />
        </Pressable>
      </View>
      <Modal transparent visible={qr} animationType="fade">
        <View style={s.modal}>
          <Card>
            <View style={s.row}>
              <Text style={s.sectionTitle}>매장 스캐너 제시용 QR</Text>
              <Pressable onPress={() => setQr(false)}>
                <X color={c.ink} />
              </Pressable>
            </View>
            <View style={s.fakeQr}>
              <QRCode value={`mcm-private-circle://customer/${customer.qrToken ?? customer.customerNo}`} size={190} color={c.paper} backgroundColor={c.ink} />
            </View>
            <Text style={s.body}>
              매장 방문 시 담당 CA에게 이 QR을 보여주세요.
            </Text>
          </Card>
        </View>
      </Modal>
    </Screen>
  );
}
function Stat({
  label,
  value,
  dark,
}: {
  label: string;
  value: string;
  dark?: boolean;
}) {
  return (
    <View style={{ flex: 1 }}>
      <Text style={[s.caption, dark && { color: "#CFC8BC" }]}>{label}</Text>
      <Text style={[s.statValue, dark && { color: c.champagne }]}>{value}</Text>
    </View>
  );
}
function SectionTitle({
  kicker = "PRIVATE CIRCLE",
  title,
  action,
}: {
  kicker?: string;
  title: string;
  action?: () => void;
}) {
  return (
    <View style={s.sectionRow}>
      <View>
        <Text style={s.kicker}>{kicker}</Text>
        <Text style={s.sectionTitle}>{title}</Text>
      </View>
      {action && (
        <Pressable onPress={action} style={s.sectionAction}>
          <Text style={s.link}>전체 보기</Text>
          <ChevronRight size={18} color={c.gold} />
        </Pressable>
      )}
    </View>
  );
}
function StoreStampImage({ storeName, size, imageUrl, lightPlate = false }: { storeName: string; size: number; imageUrl?: string; lightPlate?: boolean }) {
  const asset = getStampAsset(storeName);
  const [useFallbackAsset, setUseFallbackAsset] = useState(false);
  // 발급 카드에서는 밝은 원판과 도장을 정확히 같은 크기로 겹친다.
  const outerSize = size;
  return (
    <View style={[s.stampArtwork, { width: outerSize, height: outerSize, borderRadius: outerSize / 2 }, lightPlate && s.issueStampPlate]}>
      {imageUrl && !useFallbackAsset ? (
        <Image
          source={{ uri: imageUrl }}
          style={{ width: size, height: size }}
          resizeMode="contain"
          onError={() => setUseFallbackAsset(true)}
        />
      ) : !!asset && (
        <Image
          source={asset}
          style={{ width: size, height: size }}
          resizeMode="contain"
        />
      )}
    </View>
  );
}
function StampCard({ item, compact = false, size }: { item: JourneyStamp; compact?: boolean; size?: number }) {
  // 원본(비압축) 카드의 도장:카드 비율(94/132)을 유지해, 카드 폭이 화면에 맞춰
  // 계산되어도 도장이 찌그러지거나 과하게 작아지지 않도록 한다.
  const cardWidth = compact ? size ?? 124 : 132;
  const imageSize = compact ? Math.round(cardWidth * (94 / 132)) : 94;
  return (
    <View style={[s.stampPreview, compact && s.stampPreviewCompact, compact && { width: cardWidth }]}>
      <StoreStampImage storeName={item.storeName} size={imageSize} imageUrl={item.imageUrl} />
      <Text
        numberOfLines={compact ? 1 : 2}
        ellipsizeMode="tail"
        style={[s.stampTitle, compact && s.stampTitleCompact]}
      >
        {compact ? item.storeName : formatStoreName(item.storeName)}
      </Text>
      <Text style={s.stampDate}>{item.issuedAt.slice(0, 10)}</Text>
    </View>
  );
}
// 제품 이미지를 살짝 눌렀을 때만 아주 미세하게 확대되는, 상품이 주인공인 절제된 인터랙션이다.
function ProductImage({ source, style }: { source: string | number; style: any }) {
  const scale = useSharedValue(1);
  const imageStyle = useAnimatedStyle(() => ({ transform: [{ scale: scale.value }] }));
  return (
    <AnimatedPressable
      onPressIn={() => {
        scale.value = withTiming(1.045, { duration: 240, easing: Easing.out(Easing.quad) });
      }}
      onPressOut={() => {
        scale.value = withTiming(1, { duration: 280, easing: Easing.out(Easing.quad) });
      }}
    >
      <Animated.Image source={typeof source === "number" ? source : { uri: source }} style={[style, imageStyle]} />
    </AnimatedPressable>
  );
}
function ProductList({ products }: { products: ProductRecommendation[] }) {
  const { customer, toggleProduct } = useApp();
  const { isTablet } = useResponsive();
  return (
    <View style={[s.productList, isTablet && s.productGrid]}>
      {products.map((p, i) => (
        <View
          key={p.productId}
          style={isTablet ? s.productGridItem : undefined}
        >
          <Card delay={i * 70} style={isTablet ? s.productCardTabletShell : undefined}>
            <View style={[s.row, isTablet && s.productCardTablet]}>
              <ProductImage
                source={p.imageUrl}
                style={[s.productImage, isTablet && s.productImageTablet]}
              />
              <View style={{ flex: 1 }}>
                <Text style={s.cardTitle}>{p.productName}</Text>
                <Text style={s.body}>{p.variant}</Text>
                <Text style={s.price}>{p.price.toLocaleString()}원</Text>
              </View>
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={`${p.productName} 관심 저장`}
                onPress={() => toggleProduct(p.productId)}
              >
                <Heart
                  size={22}
                  color={
                    customer.savedProductIds.includes(p.productId)
                      ? c.wine
                      : c.muted
                  }
                  fill={
                    customer.savedProductIds.includes(p.productId)
                      ? c.wine
                      : "none"
                  }
                />
              </Pressable>
            </View>
          </Card>
        </View>
      ))}
    </View>
  );
}
function Quick({
  icon,
  imageIcon,
  title,
  onPress,
  bottomAligned = false,
}: {
  icon?: React.ReactNode;
  imageIcon?: number;
  title: string;
  onPress: () => void;
  bottomAligned?: boolean;
}) {
  const press = usePressScale();
  return (
    <AnimatedPressable
      onPress={onPress}
      onPressIn={press.onPressIn}
      onPressOut={press.onPressOut}
      style={[s.quick, bottomAligned && s.quickBottomAligned, press.style]}
    >
      {imageIcon ? <Image source={imageIcon} style={s.quickImageIcon} resizeMode="contain" /> : icon}
      <View style={bottomAligned && s.quickBottomCopy}>
        <Text style={s.cardTitle}>{title}</Text>
      </View>
    </AnimatedPressable>
  );
}

function Passport() {
  const { customer } = useApp();
  const n = useNavigation<any>();
  const [qr, setQr] = useState(false);
  const lastStamp = customer.stamps[0];
  const lastPurchase = customer.purchases[0];
  return (
    <Screen title="Journey Passport" back>
      <Card dark>
        <View style={s.passportCardTop}>
          <View style={{ flex: 1 }}>
            <Text style={s.darkKicker}>OFFICIAL DIGITAL PASSPORT</Text>
            <Text style={s.passportName}>{customer.name}</Text>
            <Text style={s.darkBody}>MEMBER NO. {customer.customerNo}</Text>
          </View>
          <Pressable onPress={() => setQr(true)} style={s.passportQrDark} accessibilityLabel="고객 식별 QR 열기">
            <QrCode size={34} color={c.champagne} />
          </Pressable>
        </View>
        <View style={s.stats}>
          <Stat dark label="MEMBERSHIP" value={customer.membershipTier} />
          <Stat dark label="가입일" value={customer.joinedAt} />
        </View>
      </Card>
      <Text style={s.summaryTitle}>최근 여정 요약</Text>
      <Card>
        <View style={s.summaryRow}><MapPin size={22} color={c.gold} style={s.summaryIcon} /><View style={s.summaryCopy}><Text style={s.caption}>마지막 방문</Text><Text style={s.cardTitle}>{lastStamp ? `${lastStamp.storeName} · ${lastStamp.issuedAt.slice(0, 7)}` : "첫 방문을 기다리고 있어요"}</Text></View></View>
        <View style={s.summaryRow}><Stamp size={22} color={c.gold} style={s.summaryIcon} /><View style={s.summaryCopy}><Text style={s.caption}>총 방문 스탬프</Text><Text style={s.cardTitle}>{customer.stamps.length}개</Text></View></View>
        {lastPurchase && <View style={s.summaryRow}><CalendarDays size={22} color={c.gold} style={s.summaryIcon} /><View style={s.summaryCopy}><Text style={s.caption}>최근 구매</Text><Text style={s.cardTitle}>{lastPurchase.purchasedAt} · {lastPurchase.productName}</Text></View></View>}
      </Card>
      <SectionTitle title="나의 국내 방문 도장" />
      <View style={s.passportStampGrid}>
        {customer.stamps.map((x) => (
          <StampCard key={x.id} item={x} />
        ))}
      </View>
      <Button onPress={() => n.navigate("Journey")}>여정 기록 전체 보기</Button>
      <Modal transparent visible={qr} animationType="fade">
        <View style={s.modal}>
          <Card>
            <View style={s.row}><Text style={s.sectionTitle}>고객 식별 QR</Text><Pressable onPress={() => setQr(false)}><X color={c.ink} size={26} /></Pressable></View>
            <View style={s.realQr}><QRCode value={`mcm-private-circle://customer/${customer.customerNo}`} size={216} color={c.ink} backgroundColor={c.paper} /></View>
            <Text style={s.body}>매장 방문 시 담당 CA에게 보여주세요.</Text>
          </Card>
        </View>
      </Modal>
    </Screen>
  );
}
function Journey() {
  const { customer } = useApp();
  const [tab, setTab] = useState<"stamps" | "records">("stamps");
  return (
    <Screen title="나의 여정" back>
      <Text style={s.kicker}>JOURNEY ARCHIVE</Text>
      <Text style={s.pageTitle}>나의 여정</Text>
      <Text style={s.journeyDescription}>방문, 구매, 케어의 순간</Text>
      <View style={s.segment}>
        <Pressable
          onPress={() => setTab("stamps")}
          style={[s.segmentItem, tab === "stamps" && s.segmentActive]}
        >
          <Text style={s.cardTitle}>방문 스탬프</Text>
        </Pressable>
        <Pressable
          onPress={() => setTab("records")}
          style={[s.segmentItem, tab === "records" && s.segmentActive]}
        >
          <Text style={s.cardTitle}>구매·케어 이력</Text>
        </Pressable>
      </View>
      {tab === "stamps" && customer.stamps.length === 0 ? (
        <EmptyJourney />
      ) : tab === "stamps" ? (
        <View style={s.journeyTimeline}>
          {customer.stamps.map((x, index) => {
            const asset = getStampAsset(x.storeName);
            return (
              <View key={x.id} style={s.journeyStop}>
                {index < customer.stamps.length - 1 && <View style={s.journeyRail} />}
                <StoreStampImage storeName={x.storeName} size={94} imageUrl={x.imageUrl} />
                <View style={s.journeyCopy}>
                  <Text style={s.journeyMonth}>{x.issuedAt.slice(0, 7).replace("-", "년 ")}월</Text>
                  <Text style={s.cardTitle}>{x.storeName}</Text>
                  <Text style={s.body}>{x.issuedAt.slice(0, 10).replace(/-/g, ". ")}</Text>
                </View>
              </View>
            );
          })}
        </View>
      ) : (
        <View style={s.recordList}>
          {customer.purchases.map((purchase) => (
            <Card key={purchase.id}>
              <View style={s.row}>
                <Image source={{ uri: purchase.imageUrl }} style={s.recordImage} />
                <View style={{ flex: 1 }}>
                  <Text style={s.cardTitle}>{purchase.productName}</Text>
                  <Text style={s.body}>{purchase.variant}</Text>
                  <Text style={s.price}>{purchase.price.toLocaleString()}원</Text>
                </View>
              </View>
              <View style={s.recordMeta}><CalendarDays size={18} color={c.gold} /><Text style={s.caption}>구매 · {purchase.purchasedAt} · {purchase.storeName ?? customer.stamps.find((stamp) => stamp.type === "purchase")?.storeName ?? customer.stamps[0]?.storeName}</Text></View>
            </Card>
          ))}
          {customer.careRecords.map((record) => (
            <Card key={record.id}>
              <Text style={s.kicker}>CARE NOTE</Text>
              <Text style={s.cardTitle}>{record.type}</Text>
              <Text style={s.body}>{record.note}</Text>
              <Text style={s.caption}>케어 · {record.date} · {record.storeName ?? "국내 MCM 매장"}</Text>
            </Card>
          ))}
        </View>
      )}
    </Screen>
  );
}
function Recommendations() {
  const { products } = useApp();
  return (
    <Screen title="맞춤 추천" back>
      <Text style={s.pageTitle}>고객 맞춤 추천 제품</Text>
      <Text style={s.recommendationDescription}>고객님만을 위한 MCM의 추천 제품입니다.</Text>
      <ProductList products={products} />
    </Screen>
  );
}
function EmptyJourney() {
  const n = useNavigation<any>();
  return <View style={s.emptyJourney}><View style={s.emptyJourneyIcon}><MapPinned size={44} color={c.forest} /></View><Text style={s.emptyJourneyTitle}>여정을 시작하세요</Text><Text style={s.emptyJourneyBody}>MCM과 함께하는 첫 번째 방문을 Journey Passport에 기록합니다.</Text><Button onPress={() => n.navigate("Passport")} icon={<BookOpen color={c.paper} size={24} />}>여권 프로필 확인</Button><Button secondary onPress={() => n.navigate("Recommendations")}>추천 제품 보기</Button></View>;
}
function Profile() {
  const { customer, logout, updateAvatar } = useApp();
  const n = useNavigation<any>();
  const pickProfilePhoto = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ["images"],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });
    if (!result.canceled) updateAvatar(result.assets[0].uri);
  };
  return (
    <Screen>
      <Text style={s.profileKicker}>MY PASSPORT</Text>
      <Text style={s.pageTitle}>나의 여권</Text>
      <Card dark>
        <View style={s.profilePassportTop}>
          <Pressable onPress={pickProfilePhoto} style={s.profilePhotoFrame}>
            <Image source={{ uri: customer.avatarUrl }} style={s.profilePhoto} />
            <View style={s.photoEdit}><Text style={s.photoEditText}>변경</Text></View>
          </Pressable>
          <View style={{ flex: 1 }}>
            <Text style={s.darkKicker}>PASSPORT HOLDER</Text>
            <Text style={s.passportName}>{customer.name}</Text>
            <Text style={s.darkBody}>MEMBER NO. {customer.customerNo}</Text>
          </View>
          {customer.membershipTier === "VIP" && <View style={s.profileVip}><Pill tone="wine">VIP</Pill></View>}
        </View>
        <View style={s.stats}>
          <Stat dark label="국내 방문" value={`${customer.stamps.length}곳`} />
          <Stat dark label="가입일" value={customer.joinedAt} />
        </View>
      </Card>
      <Button
        secondary
        onPress={() => n.navigate("Benefits")}
        icon={<Award size={18} color={c.ink} />}
      >
        멤버십 혜택
      </Button>
      <Button
        secondary
        onPress={() => n.navigate("Saved")}
        icon={<Heart size={18} color={c.ink} />}
      >
        관심 저장 제품
      </Button>
      <Pressable
        accessibilityRole="button"
        onPress={logout}
        style={s.logoutButton}
      >
        <LogOut color={c.wine} size={20} />
        <Text style={s.logoutText}>로그아웃</Text>
      </Pressable>
    </Screen>
  );
}
function Benefits() {
  return (
    <Screen title="VIP 혜택" back>
      <Text style={s.pageTitle}>Private Circle 혜택</Text>
      {[
        "우선 예약 및 전용 상담",
        "시즌별 퍼스널 큐레이션",
        "제품 케어 및 리페어 지원",
      ].map((x) => (
        <Card key={x}>
          <Award size={22} color={c.gold} />
          <Text style={s.cardTitle}>{x}</Text>
          <Text style={s.body}>
            멤버십 등급 및 매장 정책에 따라 제공됩니다.
          </Text>
        </Card>
      ))}
    </Screen>
  );
}
function Saved() {
  const { customer, products } = useApp();
  const saved = products.filter((x) =>
    customer.savedProductIds.includes(x.productId),
  );
  return (
    <Screen title="관심 저장 제품" back>
      {saved.length ? (
        <ProductList products={saved} />
      ) : (
        <Card>
          <Heart size={28} color={c.muted} />
          <Text style={s.body}>아직 저장한 제품이 없습니다.</Text>
        </Card>
      )}
    </Screen>
  );
}

function CaHome() {
  const { customers, select, currentStore, caCustomersLoading } = useApp();
  const n = useNavigation<any>();
  const { isTablet } = useResponsive();
  const clientList = (
    <>
      <SectionTitle title="최근 고객" />
      {caCustomersLoading ? (
        <Card>
          <Text style={s.cardTitle}>고객 목록을 불러오는 중입니다</Text>
          <Text style={s.body}>데이터베이스에 저장된 최근 고객 정보를 확인하고 있습니다.</Text>
        </Card>
      ) : customers.length === 0 ? (
        <Card>
          <Text style={s.cardTitle}>최근 고객이 없습니다</Text>
          <Text style={s.body}>아직 등록되었거나 조회된 고객 데이터가 없습니다.</Text>
        </Card>
      ) : customers.map((x) => (
        <Pressable
          accessibilityRole="button"
          key={x.id}
          onPress={() => {
            select(x.id);
            n.navigate("CustomerDetail", { id: x.id });
          }}
        >
          <Card>
            <View style={s.row}>
              <Image source={{ uri: x.avatarUrl }} style={s.avatar} />
              <View style={{ flex: 1 }}>
                <Text style={s.cardTitle}>{x.name} 님</Text>
                <Text style={s.body}>
                  {x.membershipTier === "VIP" ? "VIP · " : ""}스탬프{" "}
                  {x.stamps.length}개
                </Text>
              </View>
              <ChevronRight color={c.gold} />
            </View>
          </Card>
        </Pressable>
      ))}
    </>
  );
  const actions = (
    <>
      <View style={[s.caDashboardTop, isTablet && s.caDashboardTopTablet]}>
        <Pressable style={s.caScanHero} onPress={() => n.navigate("Scanner")}>
          <View style={s.caScanIcon}><QrCode color={c.ink} size={28} /></View>
          <View style={s.caScanCopy}><Text style={s.caScanTitle}>Journey Passport 스캔</Text><Text style={s.darkBody}>QR 카메라 열기</Text></View>
        </Pressable>
        <View style={s.caSearchBox}>
          <View style={s.caSearchTitle}><Search color={c.gold} size={30} /><Text style={s.sectionTitle}>고객 검색</Text></View>
          <Text style={s.body}>고객 목록과 상세 기록을 확인하는 검색 화면으로 이동합니다.</Text>
          <Button secondary onPress={() => n.navigate("Search")} icon={<Search size={22} color={c.ink} />}>고객 조회</Button>
        </View>
      </View>
      <Card>
        <Text style={s.kicker}>CURRENT STORE</Text>
        <Text style={s.cardTitle}>현재 근무 지점</Text>
        <Text style={s.body}>로그인한 CA의 소속 지점 도장이 고객 여권에 발급됩니다.</Text>
        <View style={[s.storeOption, s.storeOptionActive, { width: 150 }]}>
          <StoreStampImage storeName={currentStore} size={82} />
          <Text numberOfLines={2} style={[s.storeOptionText, s.storeOptionTextActive]}>
            {currentStore.replace("MCM ", "")}
          </Text>
        </View>
      </Card>
      <Card>
        <Text style={s.kicker}>TODAY AT A GLANCE</Text>
        <Text style={s.cardTitle}>상담 예정 고객 3명</Text>
        <Text style={s.body}>
          방문 전 여정 기록과 AI 브리프를 확인해 주세요.
        </Text>
      </Card>
    </>
  );
  return (
    <Screen preset="wide" caHeader>
      {isTablet ? (
        <View style={s.caColumns}>
          <View style={[s.caMain, s.caContentStart]}>{actions}</View>
          <View style={s.caSide}>{clientList}</View>
        </View>
      ) : (
        <>
          <View style={s.caContentStart}>{actions}</View>
          {clientList}
        </>
      )}
    </Screen>
  );
}
function Scanner() {
  const { customer, customers } = useApp();
  const n = useNavigation<any>();
  const [permission, requestPermission] = useCameraPermissions();
  const [scanned, setScanned] = useState(false);
  const hasCustomers = customers.length > 0;
  if (!permission?.granted) {
    return <Screen title="QR 카메라" back><View style={s.cameraPermission}><ScanLine size={58} color={c.gold} /><Text style={s.pageTitle}>카메라 권한이 필요합니다</Text><Text style={s.body}>고객의 Journey Passport QR을 확인하기 위해 카메라 접근을 허용해 주세요.</Text><Button onPress={() => requestPermission()}>카메라 권한 허용</Button></View></Screen>;
  }
  return (
    <Screen title="QR 스캐너" back>
      <View style={s.cameraFrame}><CameraView style={s.camera} facing="back" barcodeScannerSettings={{ barcodeTypes: ["qr"] }} onBarcodeScanned={scanned || !hasCustomers ? undefined : () => { setScanned(true); n.navigate("CustomerDetail", { id: customer.id }); }} /><View pointerEvents="none" style={s.cameraGuide} /></View>
      <Text style={s.body}>{hasCustomers ? "고객 QR을 네모 안에 맞춰 주세요." : "먼저 데이터베이스에 등록된 고객을 불러온 뒤 스캔을 진행해 주세요."}</Text>
    </Screen>
  );
}
function SearchScreen() {
  const { customers, select, syncCustomerSearchResults } = useApp();
  const n = useNavigation<any>();
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const search = async () => {
    const normalized = normalizeCustomerSearchKeyword(query);
    if (!normalized.ok) {
      Alert.alert("확인 필요", normalized.message);
      return;
    }

    setLoading(true);
    setHasSearched(true);
    try {
      const page = await customerApi.search(normalized.keyword, 0, 20);
      syncCustomerSearchResults(page.items);
    } catch (error) {
      const emptySearchMessage = getCustomerSearchErrorMessage(getApiErrorCode(error));
      if (emptySearchMessage) {
        Alert.alert("확인 필요", emptySearchMessage);
      } else {
        Alert.alert("검색 실패", getApiErrorMessage(error));
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Screen title="고객 검색" back>
      <TextInput
        placeholder="이름, 휴대폰 번호, 고객 번호"
        value={query}
        onChangeText={setQuery}
        onSubmitEditing={search}
        returnKeyType="search"
        style={s.textInput}
      />
      <Button onPress={search} icon={<Search color={c.paper} size={20} />}>
        {loading ? "검색 중" : "고객 검색"}
      </Button>
      {!hasSearched ? (
        <Card>
          <Text style={s.cardTitle}>고객을 검색해 주세요</Text>
          <Text style={s.body}>CA 고객 조회는 이름, 휴대폰 번호, 고객 번호로 서버에서 검색합니다.</Text>
        </Card>
      ) : loading ? (
        <Card>
          <Text style={s.cardTitle}>검색 중입니다</Text>
          <Text style={s.body}>고객 정보를 불러오고 있습니다.</Text>
        </Card>
      ) : customers.length === 0 ? (
        <Card>
          <Text style={s.cardTitle}>검색 결과가 없습니다</Text>
          <Text style={s.body}>실제 고객 데이터만 표시되며, 더미 고객은 노출되지 않습니다.</Text>
        </Card>
      ) : (
        customers.map((x) => (
          <Pressable
            key={x.id}
            onPress={() => {
              select(x.id);
              n.navigate("CustomerDetail", { id: x.id });
            }}
          >
            <Card>
              <Text style={s.cardTitle}>
                {x.name} · {x.customerNo}
              </Text>
              <Text style={s.body}>
                {x.membershipTier === "VIP" ? "VIP 고객" : "일반 고객"}
              </Text>
            </Card>
          </Pressable>
        ))
      )}
    </Screen>
  );
}
function CustomerDetail() {
  const { customer, syncCustomerProfile, syncCustomerStamps } = useApp();
  const n = useNavigation<any>();
  const { isTablet } = useResponsive();
  useEffect(() => {
    if (!isBackendCustomerId(customer.id)) return;
    customerApi.getById(customer.id)
      .then((profile) => {
        syncCustomerProfile(profile);
        return customerApi.stampsById(customer.id)
          .then((stamps) => syncCustomerStamps(
            customer.id,
            stamps.items.map(toFrontendJourneyStamp),
          ));
      })
      .catch(() => undefined);
  }, [customer.id]);
  const profile = (
    <>
      <Card dark>
        <View style={s.row}>
          <Image source={{ uri: customer.avatarUrl }} style={s.avatar} />
          <View>
            <Text style={s.passportName}>{customer.name} 님</Text>
            <Text style={s.darkBody}>***-****-{customer.phoneLast4}</Text>
          </View>
        </View>
        <View style={s.stats}>
          <Stat dark label="등급" value={customer.membershipTier} />
          <Stat dark label="가입일" value={customer.joinedAt} />
        </View>
      </Card>
      <View style={s.grid}>
        <Quick
          imageIcon={RECOMMEND_ICON}
          title="AI 응대 브리프"
          bottomAligned
          onPress={() => n.navigate("Brief")}
        />
        <Quick
          icon={<FileText color={c.gold} />}
          title="오늘의 상담 기록"
          onPress={() => n.navigate("Consultation")}
        />
        <Quick
          icon={<Stamp color={c.gold} />}
          title="스탬프 발급"
          onPress={() => n.navigate("IssueStamp")}
        />
        <Quick
          icon={<Heart color={c.gold} />}
          title="CA PICK 추천"
          onPress={() => n.navigate("CaRecommendations")}
        />
      </View>
    </>
  );
  const context = (
    <>
      {customer.cautionNotes && (
        <View style={s.cautionCard}>
          <Text style={[s.cardTitle, { color: c.wine }]}>
            CA 전용 응대 주의사항
          </Text>
          <Text style={s.body}>{customer.cautionNotes}</Text>
        </View>
      )}
      <SectionTitle title="상담 기록" />
      <Pressable onPress={() => n.navigate("ConsultationHistory")} style={s.historyOpenCard}>
        <FileText color={c.gold} size={26} />
        <View style={{ flex: 1 }}><Text style={s.cardTitle}>이전 상담 기록 보기</Text><Text style={s.body}>상담 날짜, 지점, 작성 CA와 세부 내용을 확인합니다.</Text></View>
        <ChevronRight color={c.gold} size={24} />
      </Pressable>
    </>
  );
  return (
    <Screen title={`${customer.name} 님 상세`} back preset="wide" caHeader>
      {isTablet ? (
        <View style={s.caColumns}>
          <View style={s.caMain}>{profile}</View>
          <View style={s.caSide}>{context}</View>
        </View>
      ) : (
        <>
          {profile}
          {context}
        </>
      )}
    </Screen>
  );
}
function ConsultationHistory() {
  const { customer } = useApp();
  const n = useNavigation<any>();
  return (
    <Screen title="이전 상담 기록" back caHeader>
      <View style={s.consultationHeading}><Text style={s.kicker}>CONSULTATION ARCHIVE</Text><Text style={s.pageTitle}>이전 상담 기록</Text><Text style={s.body}>고객 여정에 저장된 상담 기록입니다.</Text></View>
      {customer.consultations.map((note) => (
        <Card key={note.id}>
          <Text style={s.consultationMemoDate}>{note.createdAt.slice(0, 10).replace(/-/g, ". ")}</Text>
          <Text style={s.cardTitle}>{note.storeName ?? customer.stamps[0]?.storeName ?? "국내 MCM 매장"}</Text>
          <Text style={s.body}>작성 CA · {note.caName}</Text>
          <Pressable onPress={() => n.navigate("ConsultationDetail", { noteId: note.id })} style={s.historyDetailLink}><Text style={s.link}>자세히 보기</Text><ChevronRight color={c.gold} size={19} /></Pressable>
        </Card>
      ))}
      {!customer.consultations.length && <Card><Text style={s.body}>저장된 상담 기록이 없습니다.</Text></Card>}
    </Screen>
  );
}
function ConsultationDetail({ route }: { route: any }) {
  const { customer, updateConsultation, deleteConsultation } = useApp();
  const note = customer.consultations.find((item) => item.id === route.params?.noteId);
  const n = useNavigation<any>();
  const [editing, setEditing] = useState(false);
  const [purpose, setPurpose] = useState("");
  const [content, setContent] = useState("");
  const [styleChange, setStyleChange] = useState("");
  const [cautionUpdate, setCautionUpdate] = useState("");
  useEffect(() => {
    if (!note) return;
    setPurpose(note.visitPurpose);
    setContent(note.content);
    setStyleChange(note.styleChange ?? "");
    setCautionUpdate(note.cautionUpdate ?? "");
  }, [note?.id]);
  if (!note) return <Screen title="상담 기록" back caHeader><Card><Text style={s.body}>상담 기록을 찾을 수 없습니다.</Text></Card></Screen>;
  const save = async () => {
    const draft = {
      caName: note.caName,
      storeName: note.storeName ?? "국내 MCM 매장",
      visitPurpose: purpose.trim() || "상담 방문",
      content: content.trim() || "상담 내용이 입력되지 않았습니다.",
      styleChange: styleChange.trim(),
      cautionUpdate: cautionUpdate.trim(),
      consentConfirmed: note.consentConfirmed,
    };
    try {
      await caApi.updateConsultation(customer.id, note.id, note.createdAt, draft);
      updateConsultation(customer.id, note.id, draft);
      setEditing(false);
      Alert.alert("수정 완료", "상담 기록을 저장했습니다.");
    } catch (error) {
      Alert.alert("수정 실패", getApiErrorMessage(error));
    }
  };
  const remove = () => {
    const deleteRecord = async () => {
      try {
        await caApi.deleteConsultation(customer.id, note.id, note.createdAt);
        deleteConsultation(customer.id, note.id);
        n.goBack();
      } catch (error) {
        Alert.alert("삭제 실패", getApiErrorMessage(error));
      }
    };
    if (Platform.OS === "web") {
      const confirm = (globalThis as typeof globalThis & {
        confirm?: (message: string) => boolean;
      }).confirm;
      if (confirm?.("삭제한 상담 기록은 되돌릴 수 없습니다. 삭제할까요?")) {
        void deleteRecord();
      }
      return;
    }
    Alert.alert("상담 기록 삭제", "삭제한 기록은 되돌릴 수 없습니다.", [
      { text: "취소", style: "cancel" },
      { text: "삭제", style: "destructive", onPress: deleteRecord },
    ]);
  };
  return (
    <Screen title="상담 기록" back caHeader>
      <View style={s.consultationHeading}><Text style={s.kicker}>CONSULTATION ARCHIVE</Text><Text style={s.pageTitle}>상담 기록 상세</Text><Text style={s.body}>{note.createdAt.slice(0, 10)} · {note.storeName ?? "국내 MCM 매장"} · {note.caName}</Text></View>
      <Card>
        <Text style={s.formLabel}>방문 목적</Text>
        {editing ? <TextInput editable value={purpose} onChangeText={setPurpose} style={s.textInput} /> : <Text style={s.body}>{note.visitPurpose}</Text>}
        <Text style={s.formLabel}>상담 내용 및 고객 관심사</Text>
        {editing ? <TextInput editable multiline value={content} onChangeText={setContent} style={[s.textInput, s.consultationLargeInput]} /> : <Text style={s.body}>{note.content}</Text>}
        <Text style={s.formLabel}>선호 스타일 변화</Text>
        {editing ? <TextInput editable value={styleChange} onChangeText={setStyleChange} style={s.textInput} /> : <Text style={s.body}>{note.styleChange || "기록 없음"}</Text>}
        <Text style={s.formLabel}>후속 응대 시 주의사항</Text>
        {editing ? <TextInput editable multiline value={cautionUpdate} onChangeText={setCautionUpdate} style={[s.textInput, s.consultationLargeInput]} /> : <Text style={s.body}>{note.cautionUpdate || "기록 없음"}</Text>}
      </Card>
      <View style={s.detailActions}>
        {editing ? <Button onPress={save}>수정 저장</Button> : <Button secondary onPress={() => setEditing(true)}>수정</Button>}
        <Button secondary onPress={remove}>삭제</Button>
      </View>
    </Screen>
  );
}
function Brief() {
  const { customer } = useApp();
  const [brief, setBrief] = useState(MOCK_BRIEFS[customer.id]);
  const [generating, setGenerating] = useState(false);
  const [generationError, setGenerationError] = useState<string | null>(null);
  useEffect(() => {
    setBrief(MOCK_BRIEFS[customer.id]);
    setGenerationError(null);
  }, [customer.id]);
  const suggestions = brief?.mode === "LIVE AI"
    ? [brief.suggestedApproach]
    : [
        "AI 브리프를 생성하면 실제 기록을 바탕으로 응대 방향을 제안합니다.",
        "재방문 시 선호 색상과 관심 제품을 먼저 확인합니다.",
        "고객의 질문과 반응을 확인한 뒤 제품을 제안합니다.",
      ];
  return (
    <Screen title="AI 응대 브리프" back caHeader>
      <View style={s.briefHeading}>
        <Text style={s.kicker}>AI JOURNEY BRIEF</Text>
        <Text style={s.pageTitle}>오늘의 상담 브리프</Text>
        <Text style={s.body}>{customer.name} 님 · {customer.stamps[0]?.storeName ?? "국내 MCM 매장"} · 실제 여정 기록 기반</Text>
      </View>
      <View style={s.briefHero}>
        <View style={s.row}><View style={s.briefIcon}><SoundWaveIcon color={c.ink} size={30} /></View><View style={{ flex: 1 }}><Text style={s.passportName}>상담 맥락 요약</Text><Text style={s.darkBody}>{brief?.mode === "LIVE AI" ? "실제 여정 기록 기반 생성" : "브리프 생성 전"}</Text></View><Pill tone="forest">{brief?.mode === "LIVE AI" ? "LIVE AI" : "READY"}</Pill></View>
        <Text style={s.briefSummary}>{generating ? "최신 여정 기록을 분석하고 있습니다." : brief?.summary ?? "아래 버튼을 눌러 AI 브리프를 생성해 주세요."}</Text>
      </View>
      <Card>
        <View style={s.row}><SoundWaveIcon color={c.gold} size={24} /><Text style={s.sectionTitle}>응대 제안</Text></View>
        {suggestions.map((item, index) => <View key={item} style={s.briefSuggestion}><View style={s.briefNumber}><Text style={s.briefNumberText}>{index + 1}</Text></View><Text style={s.body}>{item}</Text></View>)}
      </Card>
      <Card>
        <View style={s.row}><FileText color={c.gold} size={23} /><Text style={s.cardTitle}>근거 기록</Text></View>
        <Text style={s.body}>{brief?.basis.slice(0, 3).join(" · ")}</Text>
      </Card>
      <View style={s.cautionCard}>
        <Text style={[s.cardTitle, { color: c.wine }]}>응대 시 주의사항</Text>
        <Text style={s.body}>{brief?.cautions[0] ?? "고객의 반응을 먼저 확인해 주세요."}</Text>
      </View>
      <View style={s.aiDisclosure}><Text style={s.aiDisclosureText}>AI는 고객과 직접 대화하지 않습니다. 이 브리프는 실제 기록을 요약한 참고 정보이며 최종 응대 방식은 CA가 결정합니다.</Text></View>
      {generationError && <View style={s.aiDisclosure}><Text style={s.aiDisclosureText}>{generationError}</Text></View>}
      {isBackendCustomerId(customer.id) ? (
        <Button secondary disabled={generating} onPress={async () => {
          if (generating) return;
          setGenerating(true);
          setGenerationError(null);
          try {
            const response = await caApi.regenerateCustomerInsights(customer.id);
            setBrief(response.brief);
            Alert.alert("AI 브리프 갱신 완료", "최신 상담 기록과 이전 여정을 반영했습니다.");
          } catch (error) {
            const message = getApiErrorMessage(error);
            setGenerationError(`AI 브리프 생성 실패: ${message}`);
            Alert.alert("갱신 실패", message);
          } finally {
            setGenerating(false);
          }
        }}>{generating ? "AI 브리프 생성 중..." : "최신 기록으로 다시 생성"}</Button>
      ) : (
        <View style={s.aiDisclosure}>
          <Text style={s.aiDisclosureText}>데모 고객은 AI 브리프 생성 버튼을 숨겼습니다. 실제 고객을 선택하면 생성 버튼이 표시됩니다.</Text>
        </View>
      )}
    </Screen>
  );
}
function CaRecommendations() {
  const { products } = useApp();
  return (
    <Screen title="CA PICK 추천" back>
      <Text style={s.body}>
        고객 선호와 상담 맥락을 토대로 제안할 제품 후보입니다.
      </Text>
      <ProductList products={products.slice(0, 3)} />
    </Screen>
  );
}
function Consultation() {
  const n = useNavigation<any>();
  const { isTablet } = useResponsive();
  const { customer, currentStore, currentCaName, addConsultation } = useApp();
  const [purpose, setPurpose] = useState("");
  const [memo, setMemo] = useState("");
  const [products, setProducts] = useState("");
  const [style, setStyle] = useState("");
  const [caution, setCaution] = useState("");
  const [consented, setConsented] = useState(false);
  return (
    <Screen title="상담 기록 작성" back caHeader>
      <View style={s.consultationHeading}><Text style={s.kicker}>CONSULTATION RECORD</Text><Text style={s.pageTitle}>상담 기록 작성</Text><Text style={s.body}>고객님의 다음 응대에 필요한 정성적 맥락만 정확하게 기록합니다.</Text></View>
      <Card>
        <Text style={s.formLabel}>방문 목적</Text>
        <TextInput editable keyboardType="default" value={purpose} onChangeText={setPurpose} placeholder="관심 제품 비교 및 착용감 확인" placeholderTextColor={c.muted} style={s.textInput} />
        <Text style={s.formLabel}>상담 내용 및 고객 관심사</Text>
        <TextInput editable keyboardType="default" multiline value={memo} onChangeText={setMemo} placeholder="오늘 상담에서 나눈 주요 내용과 반응" placeholderTextColor={c.muted} style={[s.textInput, s.consultationLargeInput]} />
        <Text style={s.formLabel}>관심 제품</Text>
        <TextInput editable keyboardType="default" value={products} onChangeText={setProducts} placeholder="제품명 또는 카테고리" placeholderTextColor={c.muted} style={s.textInput} />
        <Text style={s.formLabel}>선호 스타일 변화</Text>
        <TextInput editable keyboardType="default" value={style} onChangeText={setStyle} placeholder="이전 방문과 달라진 취향" placeholderTextColor={c.muted} style={s.textInput} />
        <Text style={s.formLabel}>후속 응대 시 주의사항</Text>
        <TextInput editable keyboardType="default" multiline value={caution} onChangeText={setCaution} placeholder="다음 CA가 반드시 알아야 할 사항" placeholderTextColor={c.muted} style={[s.textInput, s.consultationLargeInput]} />
      </Card>
      <Pressable onPress={() => setConsented(!consented)} style={s.consentBox}><View style={[s.checkbox, consented && s.checkboxChecked]}>{consented && <Text style={s.checkboxTick}>✓</Text>}</View><View style={{ flex: 1 }}><Text style={s.cardTitle}>입력 내용 검토</Text><Text style={s.body}>{isTablet ? "기록은 상담 지원과 고객 여정에만 활용하며 인사평가나 성과 보상에는 사용하지 않습니다." : "기록은 상담 지원과 고객 여정에만 활용하며\n인사평가나 성과 보상에는 사용하지 않습니다."}</Text></View></Pressable>
      <Button
        onPress={async () => {
          if (!consented) { Alert.alert("확인 필요", "입력 내용 검토에 동의해 주세요."); return; }
          const draft = {
            caName: `${currentCaName} CA`,
            storeName: currentStore,
            visitPurpose: purpose || "상담 방문",
            content: memo || "상담 내용이 입력되지 않았습니다.",
            styleChange: style,
            cautionUpdate: caution,
            consentConfirmed: true,
          };
          try {
            const saved = await caApi.createConsultation(customer.id, draft);
            addConsultation(customer.id, draft, saved.visitRecordId);
            Alert.alert("저장 완료", "상담 기록이 고객 이력에 저장되었습니다.");
            n.goBack();
          } catch {
            Alert.alert("저장 실패", "네트워크를 확인한 뒤 다시 시도해 주세요.");
          }
        }}
      >
        상담 기록 저장
      </Button>
    </Screen>
  );
}
function IssueStamp() {
  const { customer, addStamp, currentStore, currentCaName } = useApp();
  const n = useNavigation<any>();
  const { isTablet } = useResponsive();
  const [verified, setVerified] = useState(false);
  return (
    <Screen title="방문 스탬프 발급" back preset="wide" caHeader>
      <View style={s.issueHeading}><Text style={s.kicker}>JOURNEY STAMP</Text><Text style={s.pageTitle}>방문 스탬프 발급</Text><Text style={s.body}>{isTablet ? "고객, 매장, 담당 CA와 발급 일시가 실제 방문과 일치하는지 확인합니다." : "고객, 매장, 담당 CA와 발급 일시가 실제 방문과 일치하는지 확인"}</Text></View>
      <View style={[s.issueDetailColumns, !isTablet && s.issueDetailColumnsMobile]}>
        <View style={s.issueVisual}><StoreStampImage storeName={currentStore} size={160} lightPlate /><Text style={s.issueVisualStore}>{currentStore}</Text><Text style={s.issueVisualCaption}>OFFICIAL JOURNEY STAMP</Text></View>
        <View style={s.issueInfoColumn}><View style={[s.card, s.issueInfoCard]}><Text style={[s.label, s.issueInfoLabel]}>발급 대상 고객</Text><Text style={[s.cardTitle, s.issueInfoTitle]}>{customer.name} · {customer.membershipTier === "VIP" ? "VIP 고객" : "일반 고객"}</Text><View style={s.issueDetailLine}><MapPin size={16} color={c.gold} /><View><Text style={[s.caption, s.issueInfoCaption]}>방문 매장</Text><Text style={[s.cardTitle, s.issueInfoTitle]}>{currentStore}</Text></View></View><View style={s.issueDetailLine}><View><Text style={[s.caption, s.issueInfoCaption]}>담당 CA</Text><Text style={[s.cardTitle, s.issueInfoTitle]}>{currentCaName} 어드바이저</Text></View></View><View><Text style={[s.caption, s.issueInfoCaption]}>발급 일시</Text><Text style={[s.cardTitle, s.issueInfoTitle]}>{new Date().toLocaleString("ko-KR")}</Text></View></View></View>
      </View>
      <View style={[s.issueActions, !isTablet && s.issueActionsMobile]}>
        <View style={[s.issueAction, s.issueVisualAction]}><Button onPress={async () => { if (!verified) { setVerified(true); return; } try { await caApi.issueVisitStamp(customer.id); addStamp(customer.id, "visit"); n.navigate("StampSuccess"); } catch (error) { Alert.alert("발급 실패", getApiErrorMessage(error)); } }} icon={<Stamp color={c.paper} size={22} />}>{verified ? "방문 스탬프 발급" : "중복 발급 여부 확인"}</Button></View>
        <View style={[s.issueAction, s.issueInfoAction]}><Button secondary onPress={() => n.goBack()}>취소</Button></View>
      </View>
    </Screen>
  );
}
function Unregistered() {
  const n = useNavigation<any>();
  return (
    <Screen title="고객 확인" back>
      <Card>
        <Text style={s.pageTitle}>등록되지 않은 QR입니다</Text>
        <Text style={s.body}>
          고객 번호를 검색하거나 Private Circle 가입을 안내해 주세요.
        </Text>
      </Card>
      <Button secondary onPress={() => n.navigate("Search")}>
        고객 검색으로 이동
      </Button>
    </Screen>
  );
}
function StampSuccess() {
  const n = useNavigation<any>();
  return (
    <Screen title="스탬프 발급 완료" back>
      <Card dark>
        <Text style={s.darkKicker}>JOURNEY UPDATED</Text>
        <Text style={s.passportName}>스탬프가 발급되었습니다</Text>
        <Text style={s.darkBody}>
          고객의 Journey Passport에 바로 반영되었습니다.
        </Text>
      </Card>
      <Button onPress={() => n.navigate("CaHome")}>CA 홈으로</Button>
    </Screen>
  );
}

const Tabs = createBottomTabNavigator();
const Stack = createNativeStackNavigator();
function CustomerTabs() {
  return (
    <Tabs.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: c.ink,
        tabBarInactiveTintColor: "#8A847C",
        tabBarStyle: { height: 76, paddingTop: 7, paddingBottom: 9 },
        tabBarItemStyle: { paddingVertical: 2 },
        tabBarLabelStyle: { marginBottom: 2 },
        tabBarIcon: ({ color, size }) => {
          if (route.name === "Recommendations") {
            return <Image source={RECOMMEND_ICON} style={{ width: size + 2, height: size + 2, tintColor: color }} resizeMode="contain" />;
          }
          const icons: any = {
            Home,
            Passport: BookOpen,
            Journey: MapPinned,
            Profile: UserRound,
          };
          const I = icons[route.name];
          return <I color={color} size={size} />;
        },
      })}
    >
      <Tabs.Screen
        name="Home"
        component={CustomerHome}
        options={{ title: "홈" }}
      />
      <Tabs.Screen
        name="Passport"
        component={Passport}
        options={{ title: "여권" }}
      />
      <Tabs.Screen
        name="Journey"
        component={Journey}
        options={{ title: "여정" }}
      />
      <Tabs.Screen
        name="Recommendations"
        component={Recommendations}
        options={{ title: "추천" }}
      />
      <Tabs.Screen
        name="Profile"
        component={Profile}
        options={{ title: "마이" }}
      />
    </Tabs.Navigator>
  );
}
function CustomerFlow() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="CustomerTabs" component={CustomerTabs} />
      <Stack.Screen name="Benefits" component={Benefits} />
      <Stack.Screen name="Saved" component={Saved} />
    </Stack.Navigator>
  );
}
function CaFlow() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="CaHome" component={CaHome} />
      <Stack.Screen name="Scanner" component={Scanner} />
      <Stack.Screen name="Search" component={SearchScreen} />
      <Stack.Screen name="Unregistered" component={Unregistered} />
      <Stack.Screen name="CustomerDetail" component={CustomerDetail} />
      <Stack.Screen name="Brief" component={Brief} />
      <Stack.Screen name="ConsultationHistory" component={ConsultationHistory} />
      <Stack.Screen name="ConsultationDetail" component={ConsultationDetail} />
      <Stack.Screen name="CaRecommendations" component={CaRecommendations} />
      <Stack.Screen name="Consultation" component={Consultation} />
      <Stack.Screen name="IssueStamp" component={IssueStamp} />
      <Stack.Screen name="StampSuccess" component={StampSuccess} />
    </Stack.Navigator>
  );
}
function Root() {
  const { role, isLoggedIn, authScreen, caCustomersLoading } = useApp();
  return (
    <NavigationContainer>
      {!isLoggedIn ? (
        authScreen === "signup" ? (
          <SignUp />
        ) : (
          <Login />
        )
      ) : role === "customer" ? (
        <CustomerFlow />
      ) : caCustomersLoading ? (
        <Screen preset="wide" caHeader>
          <Card>
            <Text style={s.cardTitle}>고객 데이터를 준비하고 있습니다</Text>
            <Text style={s.body}>CA 계정에서는 데이터베이스에 저장된 고객만 불러옵니다.</Text>
          </Card>
        </Screen>
      ) : (
        <CaFlow />
      )}
    </NavigationContainer>
  );
}
export default function App() {
  const [showSplash, setShowSplash] = useState(true);
  // 폰트 다운로드가 느려도 첫 화면이 멈추지 않도록 백그라운드에서만 준비한다.
  useFonts({
    Pretendard: require("../assets/fonts/Pretendard-Regular.otf"),
    "Pretendard-SemiBold": require("../assets/fonts/Pretendard-SemiBold.otf"),
    "Pretendard-Bold": require("../assets/fonts/Pretendard-Bold.otf"),
  });
  return (
    <SafeAreaProvider>
      {showSplash ? (
        <Splash onComplete={() => setShowSplash(false)} />
      ) : (
        <Provider>
          <Root />
        </Provider>
      )}
    </SafeAreaProvider>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: c.bg },
  scrollOuter: { flexGrow: 1, alignItems: "center", paddingBottom: 18 },
  scroll: { width: "100%", padding: 20, gap: 24, paddingBottom: 20 },
  splash: { flex: 1, backgroundColor: "#12100E" },
  splashPress: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 32,
  },
  splashLogo: { width: "88%", maxWidth: 500, height: 178 },
  login: { backgroundColor: c.ink },
  loginTablet: { flexDirection: "row" },
  loginDark: { height: 348, flexGrow: 0, flexShrink: 0, paddingHorizontal: 24, paddingTop: 24, paddingBottom: 26, backgroundColor: c.ink },
  loginDarkTablet: { width: "42%", flexGrow: 0, flexShrink: 0, height: undefined, paddingTop: 68, paddingBottom: 52, paddingLeft: 46, paddingRight: 24 },
  loginInner: { flex: 1, maxWidth: 460, alignSelf: "center", width: "100%", justifyContent: "flex-start" },
  loginInnerTablet: { maxWidth: 420 },
  // 휴대폰에서도 logo-tight.png 파일 내부의 투명 여백까지 보정해, 실제 로고 픽셀 좌측 끝이
  // 아래 제목/본문 텍스트의 좌측 시작선과 정확히 일직선이 되도록 왼쪽 정렬한다.
  loginLogo: { width: 326, height: 123, alignSelf: "flex-start", marginLeft: -8, marginTop: 8 },
  // 실제 로고 픽셀은 잘리지 않는다.
  // 태블릿: 로그인 본문보다 로고가 안쪽으로 보이던 여백을 명확히 제거한다.
  // 태블릿 로그인: 이전처럼 여백을 두어 로고가 왼쪽에 붙지 않게 한다.
  loginLogoTablet: { width: 370, height: 142, alignSelf: "flex-start", marginLeft: -9, marginTop: 0 },
  // 로고와 Journey Passport 배지/본문은 서로 충분히 떨어뜨린다.
  loginHeroSpacer: { height: 30 },
  loginHeroSpacerTablet: { height: 30 },
  loginForm: {
    flexGrow: 1,
    backgroundColor: c.paper,
    paddingVertical: 38,
    gap: 24,
  },
  loginFormTablet: {
    flex: 1,
    justifyContent: "flex-start",
    paddingTop: 72,
    paddingBottom: 72,
  },
  loginFormInner: {
    width: "100%",
    maxWidth: 560,
    alignSelf: "center",
    gap: 20,
  },
  authField: { gap: 10, marginTop: 5 },
  passwordField: { marginTop: 30 },
  loginButtonWrap: { marginTop: 34 },
  authTopRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
  },
  loginIntro: { marginBottom: 34 },
  // 로그인 히어로: 헤드라인과 설명 문구 사이를 조금 더 벌려 위계를 분명히 한다.
  loginHeroBody: { marginTop: 12 },
  // 로그인 타이틀: WELCOME BACK 배지와 더 가깝게 붙여 하나의 그룹으로 보이게 하고,
  // "로그인"은 살짝 더 크게 강조한다.
  loginTitle: { fontSize: 28, lineHeight: 36 },
  roleSwitch: {
    minWidth: 58,
    height: 44,
    borderWidth: 1,
    borderColor: c.line,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 4,
    backgroundColor: c.paper,
  },
  roleSwitchActive: { backgroundColor: c.ink, borderColor: c.ink },
  roleSwitchText: { color: c.ink, fontFamily: "Pretendard-Bold", fontWeight: "800", fontSize: 11 },
  roleSwitchTextActive: { color: c.paper },
  signupLink: {
    flexDirection: "row",
    justifyContent: "center",
    paddingVertical: 12,
  },
  signupText: { color: c.muted, fontFamily: "Pretendard", fontSize: 13 },
  signupLinkText: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 13, fontWeight: "800" },
  signupOuter: { flexGrow: 1, alignItems: "center", padding: 24 },
  signupInner: { width: "100%", maxWidth: 560, gap: 18, paddingTop: 22 },
  backText: {
    alignSelf: "flex-start",
    minHeight: 52,
    justifyContent: "center",
  },
  logo: { color: c.paper, fontFamily: "Pretendard-Bold", fontWeight: "900", fontSize: 32, letterSpacing: 6 },
  logoSub: {
    color: c.champagne,
    fontSize: 10,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    letterSpacing: 3,
  },
  logoSubGold: {
    color: c.gold,
    fontSize: 9,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    letterSpacing: 2,
  },
  loginHeadline: {
    color: c.paper,
    fontSize: 24,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    lineHeight: 33,
    marginTop: 20,
  },
  loginHeadlineTablet: { fontSize: 30, lineHeight: 40 },
  pageTitle: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 25, fontWeight: "800", lineHeight: 34 },
  body: { color: c.muted, fontFamily: "Pretendard", fontSize: 14, lineHeight: 21 },
  darkBody: { color: "#D5D0C8", fontFamily: "Pretendard", fontSize: 13, lineHeight: 20, marginTop: 6 },
  kicker: {
    color: c.gold,
    fontSize: 11,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    letterSpacing: 1,
    marginBottom: 4,
  },
  loginKicker: { marginBottom: 4 },
  signupKicker: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 13, fontWeight: "800", letterSpacing: 1.25, marginBottom: -8 },
  profileKicker: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 14, fontWeight: "800", letterSpacing: 1.4, marginBottom: -12 },
  darkKicker: {
    color: c.champagne,
    fontSize: 10,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    letterSpacing: 1.5,
  },
  brandRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    borderBottomWidth: 1,
    borderColor: c.line,
    paddingBottom: 14,
  },
  homeBrandRow: { marginTop: -21, marginLeft: -36, marginBottom: 7, paddingBottom: 1 },
  homeBrandRowPhone: { marginTop: -28, marginBottom: 0, paddingBottom: 0 },
  homeLogoPlate: { width: 258, height: 96, position: "relative" },
  homeLogoPlatePhone: { width: 282, height: 100 },
  homeLogo: { width: 258, height: 96 },
  homeLogoPhone: { width: 282, height: 100 },
  homeLogoShadow: { position: "absolute", width: 258, height: 96, tintColor: c.ink, opacity: 0.78, transform: [{ translateX: 1.5 }, { translateY: 1.5 }] },
  homeGreeting: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 25, fontWeight: "800", lineHeight: 32, marginTop: -10, marginBottom: -15 },
  brand: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 25, fontWeight: "900", letterSpacing: 4 },
  card: {
    backgroundColor: c.paper,
    borderColor: c.line,
    borderWidth: 1,
    borderRadius: 8,
    padding: 20,
    gap: 12,
    // 카드가 배경 위에 살짝 떠 있는 정도의, 있는 듯 없는 듯한 그림자.
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 2,
  },
  darkCard: { backgroundColor: c.ink, borderColor: "#4A4640", shadowOpacity: 0.16 },
  passportName: {
    color: c.paper,
    fontSize: 22,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    marginTop: 4,
  },
  row: { flexDirection: "row", alignItems: "center", gap: 10 },
  stats: {
    flexDirection: "row",
    gap: 12,
    borderTopWidth: 1,
    borderColor: "#4A4640",
    paddingTop: 14,
    marginTop: 10,
  },
  homeStats: { gap: 0 },
  homeStatsSpacer: { width: 12 },
  homeJoinedStat: { flex: 1, transform: [{ translateX: 12 }] },
  caption: { color: c.muted, fontFamily: "Pretendard", fontSize: 11 },
  statValue: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 16, fontWeight: "800", marginTop: 3 },
  pill: {
    paddingHorizontal: 9,
    paddingVertical: 5,
    borderRadius: 20,
    alignSelf: "flex-start",
  },
  pillText: { fontFamily: "Pretendard-Bold", fontWeight: "800", fontSize: 10 },
  button: {
    backgroundColor: c.ink,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    paddingHorizontal: 16,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 6,
    elevation: 2,
  },
  buttonSecondary: {
    backgroundColor: c.paper,
    borderWidth: 1,
    borderColor: c.line,
    shadowOpacity: 0.04,
    elevation: 1,
  },
  buttonContent: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
  },
  buttonText: { color: c.paper, fontFamily: "Pretendard-Bold", fontSize: 14, fontWeight: "700" },
  buttonTextSecondary: { color: c.ink },
  logoutButton: {
    minHeight: 42,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
    marginTop: 6,
  },
  logoutText: { color: c.wine, fontFamily: "Pretendard-Bold", fontWeight: "800", fontSize: 14 },
  sectionRow: {
    flexDirection: "row",
    alignItems: "flex-end",
    justifyContent: "space-between",
    marginTop: 18,
  },
  sectionTitle: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 18, fontWeight: "800" },
  link: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 14, fontWeight: "800" },
  sectionAction: { minHeight: 40, flexDirection: "row", alignItems: "center", gap: 1, justifyContent: "center" },
  stampPreview: { width: 132, alignItems: "center", gap: 7, paddingVertical: 6, paddingHorizontal: 5 },
  stampPreviewCompact: { gap: 5, paddingHorizontal: 3 },
  stampImage: { width: 94, height: 94 },
  stampArtwork: { alignItems: "center", justifyContent: "center", overflow: "hidden" },
  stampArtworkFallback: { ...StyleSheet.absoluteFillObject, borderWidth: 1.5, borderColor: c.wine, alignItems: "center", justifyContent: "center", backgroundColor: "#FFFDF9" },
  stampFallback: { width: 94, height: 94, borderRadius: 47, borderWidth: 1.5, borderColor: c.wine, alignItems: "center", justifyContent: "center" },
  stampFallbackCompact: { borderWidth: 1.5, borderColor: c.wine, alignItems: "center", justifyContent: "center" },
  stampTitle: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 13, fontWeight: "700", textAlign: "center", lineHeight: 18, minHeight: 36 },
  stampTitleCompact: { fontSize: 11, lineHeight: 14, minHeight: 14 },
  stampDate: { color: c.wine, fontFamily: "Pretendard-Bold", fontSize: 10, fontWeight: "700" },
  passportOwnerRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", paddingHorizontal: 4 },
  passportQr: { width: 70, height: 70, borderRadius: 12, borderWidth: 1, borderColor: c.gold, alignItems: "center", justifyContent: "center" },
  passportStampGrid: { flexDirection: "row", flexWrap: "wrap", rowGap: 26, columnGap: 0, justifyContent: "space-evenly" },
  passportCardTop: { flexDirection: "row", gap: 12, alignItems: "flex-start" },
  passportQrDark: { width: 62, height: 62, borderRadius: 10, borderWidth: 1, borderColor: c.gold, alignItems: "center", justifyContent: "center" },
  summaryTitle: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 20, fontWeight: "800", marginTop: 2, marginBottom: -10 },
  summaryRow: { flexDirection: "row", gap: 14, alignItems: "flex-start", paddingVertical: 9, borderBottomWidth: 1, borderColor: c.line },
  summaryIcon: { marginTop: 8 },
  summaryCopy: { flex: 1, marginTop: -2 },
  realQr: { alignItems: "center", paddingVertical: 20, backgroundColor: c.paper },
  productList: { gap: 12 },
  productGrid: { flexDirection: "row", flexWrap: "wrap", gap: 12 },
  productGridItem: { width: "32%" },
  productCardTabletShell: { height: 340 },
  productCardTablet: { flex: 1, flexDirection: "column", alignItems: "stretch", justifyContent: "space-between" },
  productImage: {
    width: 70,
    height: 70,
    borderRadius: 6,
    backgroundColor: c.cloud,
  },
  productImageTablet: { width: "100%", height: 150 },
  cardTitle: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 15, fontWeight: "800" },
  price: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 13, fontWeight: "800", marginTop: 5 },
  // 두 카드가 프로필 카드와 같은 좌우 끝선을 공유하도록 고정 gap 대신 남는 폭을 분배한다.
  grid: { flexDirection: "row", flexWrap: "wrap", justifyContent: "space-between", rowGap: 14 },
  quick: {
    width: "49.4%",
    minHeight: 128,
    backgroundColor: c.paper,
    borderWidth: 1,
    borderColor: c.line,
    borderRadius: 8,
    padding: 16,
    gap: 12,
    justifyContent: "space-between",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.045,
    shadowRadius: 8,
    elevation: 1,
  },
  quickBottomAligned: { justifyContent: "space-between", minHeight: 128 },
  quickBottomCopy: { gap: 2 },
  quickImageIcon: { width: 24, height: 24, tintColor: c.gold },
  homeActionList: { gap: 12, marginTop: 2 },
  homeActionDark: { minHeight: 66, borderRadius: 8, backgroundColor: c.ink, paddingHorizontal: 18, flexDirection: "row", gap: 14, alignItems: "center" },
  homeActionDarkText: { flex: 1, color: c.paper, fontFamily: "Pretendard-Bold", fontSize: 17, fontWeight: "800" },
  homeActionLight: { minHeight: 66, borderRadius: 8, backgroundColor: c.paper, borderWidth: 1, borderColor: c.line, paddingHorizontal: 18, flexDirection: "row", gap: 14, alignItems: "center" },
  homeActionLightText: { flex: 1, color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 17, fontWeight: "800" },
  caColumns: { flexDirection: "row", gap: 20, alignItems: "flex-start" },
  caContentStart: { paddingTop: 18, gap: 16 },
  caMain: { flex: 1.15, gap: 16 },
  caSide: { flex: 0.85, gap: 16 },
  modal: {
    flex: 1,
    backgroundColor: "rgba(25,23,20,.78)",
    justifyContent: "center",
    padding: 22,
  },
  fakeQr: {
    height: 180,
    backgroundColor: c.ink,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 6,
  },
  header: {
    height: 66,
    paddingHorizontal: 16,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    backgroundColor: c.ink,
  },
  headerHome: { height: 92, paddingHorizontal: 4 },
  headerMark: {
    width: 42,
    height: 42,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    backgroundColor: c.darkPanel,
  },
  // 헤더 높이를 줄이는 대신 로고가 세로 공간을 더 채우도록 한다.
  headerLogoMark: { width: 186, height: 64, backgroundColor: "transparent" },
  // 로고 원본 비율(1820:650)과 맞춰 letterboxing 없이 렌더링한다.
  headerHomeMark: { width: 232, height: 83 },
  headerLogo: { width: 180, height: 64 },
  // 휴대폰 홈: 큰 로고의 보이는 왼쪽 끝을 본문 시작선과 맞춘다.
  headerHomeLogo: { width: 232, height: 83, marginLeft: 4 },
  headerMarkText: { color: c.champagne, fontFamily: "Pretendard-Bold", fontWeight: "900", fontSize: 30, lineHeight: 34 },
  headerKicker: {
    color: c.champagne,
    fontSize: 9,
    fontFamily: "Pretendard-Bold", fontWeight: "800",
    letterSpacing: 1,
  },
  headerTitle: { color: c.paper, fontFamily: "Pretendard-Bold", fontSize: 16, fontWeight: "800" },
  caHeaderLogo: { width: 180, height: 64 },
  caHeaderLogoPhone: { marginLeft: -24 },
  caHeaderIdentity: { minWidth: 0, marginLeft: "auto", flexDirection: "row", alignItems: "center", justifyContent: "flex-end", gap: 7 },
  caHeaderName: { color: c.paper, fontFamily: "Pretendard-Bold", fontWeight: "800", fontSize: 15 },
  caHeaderStore: { color: "#CFC8BC", fontFamily: "Pretendard", fontSize: 11, marginTop: 2 },
  caHeaderLogout: { width: 29, height: 29, borderRadius: 7, backgroundColor: c.paper, alignItems: "center", justifyContent: "center" },
  segment: {
    flexDirection: "row",
    backgroundColor: "#ECEAE6",
    padding: 4,
    borderRadius: 8,
  },
  segmentItem: { flex: 1, paddingVertical: 13, alignItems: "center", borderRadius: 6 },
  segmentActive: { backgroundColor: c.paper },
  label: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 12, fontWeight: "800", marginTop: 4 },
  input: {
    minHeight: 48,
    borderWidth: 1,
    borderColor: c.line,
    borderRadius: 8,
    paddingHorizontal: 14,
    justifyContent: "center",
    backgroundColor: "#FAFAF8",
  },
  demo: { color: c.muted, fontFamily: "Pretendard", fontSize: 12, textAlign: "center" },
  avatar: { width: 52, height: 52, borderRadius: 26, backgroundColor: c.cloud },
  scanner: {
    minHeight: 280,
    backgroundColor: c.ink,
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    gap: 16,
  },
  textInput: {
    minHeight: 54,
    borderWidth: 1,
    borderColor: c.line,
    borderRadius: 8,
    backgroundColor: c.paper,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: 14,
    fontFamily: "Pretendard",
    color: c.ink,
  },
  profilePassportTop: { flexDirection: "row", alignItems: "center", gap: 16, position: "relative", paddingRight: 48 },
  profilePhotoFrame: { width: 82, height: 102, borderWidth: 1, borderColor: c.champagne, padding: 5 },
  profilePhoto: { width: "100%", height: "100%", resizeMode: "cover" },
  photoEdit: { position: "absolute", right: 3, bottom: 3, backgroundColor: c.ink, paddingHorizontal: 5, paddingVertical: 3 },
  photoEditText: { color: c.paper, fontFamily: "Pretendard-Bold", fontSize: 9, fontWeight: "800" },
  profileVip: { position: "absolute", right: 0, top: 0 },
  journeyDescription: { color: c.muted, fontFamily: "Pretendard", fontSize: 14, lineHeight: 21, marginTop: -14 },
  recommendationDescription: { color: c.muted, fontFamily: "Pretendard", fontSize: 14, lineHeight: 21, marginTop: -14 },
  journeyTimeline: { paddingTop: 4, gap: 0 },
  journeyStop: { minHeight: 128, flexDirection: "row", alignItems: "center", gap: 16, paddingLeft: 8 },
  journeyRail: { position: "absolute", left: 55, top: 105, bottom: -23, width: 1.5, backgroundColor: c.line },
  journeyStampImage: { width: 94, height: 94, zIndex: 1 },
  journeyDot: { width: 82, height: 82, borderRadius: 41, borderWidth: 1.5, borderColor: c.wine, alignItems: "center", justifyContent: "center", zIndex: 1 },
  journeyCopy: { flex: 1, gap: 3 },
  journeyMonth: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 11, fontWeight: "800" },
  recordList: { gap: 14 },
  recordImage: { width: 72, height: 72, borderRadius: 8, backgroundColor: c.cloud },
  recordMeta: { flexDirection: "row", alignItems: "center", gap: 7, borderTopWidth: 1, borderColor: c.line, paddingTop: 12, marginTop: 4 },
  storePicker: { paddingTop: 4 },
  storeOption: { minHeight: 132, alignItems: "center", justifyContent: "center", gap: 7, padding: 10, borderWidth: 1, borderColor: c.line, borderRadius: 10, backgroundColor: "#FCFAF7" },
  storeOptionActive: { borderColor: c.gold, backgroundColor: "#F7EFD9" },
  storeOptionText: { color: c.muted, fontFamily: "Pretendard-Bold", fontSize: 10, fontWeight: "700", textAlign: "center", lineHeight: 14 },
  storeOptionTextActive: { color: c.ink },
  issueStoreRow: { flexDirection: "row", alignItems: "center", gap: 14, padding: 16, borderWidth: 1, borderColor: c.line, borderRadius: 10, backgroundColor: "#FCFAF7" },
  issueStoreStamp: { width: 76, height: 76 },
  emptyJourney: { alignItems: "center", paddingVertical: 42, gap: 18 },
  emptyJourneyIcon: { width: 96, height: 96, borderRadius: 48, alignItems: "center", justifyContent: "center", backgroundColor: "#E4EEEA" },
  emptyJourneyTitle: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 26, fontWeight: "800" },
  emptyJourneyBody: { color: c.muted, fontFamily: "Pretendard", fontSize: 14, lineHeight: 22, textAlign: "center", marginBottom: 10 },
  caDashboardTop: { gap: 24 },
  caDashboardTopTablet: { flexDirection: "row", alignItems: "stretch" },
  caScanHero: { flex: 1, minHeight: 200, padding: 22, backgroundColor: c.ink, justifyContent: "flex-start", gap: 18, borderRadius: 8, flexDirection: "column", alignItems: "flex-start" },
  caScanIcon: { width: 60, height: 60, borderRadius: 12, backgroundColor: c.paper, alignItems: "center", justifyContent: "center" },
  caScanCopy: { gap: 1 },
  caScanTitle: { color: c.paper, fontFamily: "Pretendard-Bold", fontSize: 21, fontWeight: "800" },
  caSearchBox: { flex: 0.85, minHeight: 200, padding: 20, borderWidth: 1, borderColor: c.line, borderRadius: 8, gap: 14, backgroundColor: c.paper },
  caSearchTitle: { flexDirection: "row", alignItems: "center", gap: 10 },
  cameraPermission: { alignItems: "center", justifyContent: "center", minHeight: 420, gap: 18, padding: 28 },
  cameraFrame: { height: 360, borderRadius: 12, overflow: "hidden", backgroundColor: c.ink },
  camera: { flex: 1 },
  cameraGuide: { position: "absolute", width: "68%", aspectRatio: 1, alignSelf: "center", top: "16%", borderWidth: 2, borderColor: c.champagne, borderRadius: 12 },
  cautionCard: { backgroundColor: "#F8EDEF", borderWidth: 1, borderColor: "#D7A9AF", borderRadius: 8, padding: 20, gap: 12 },
  consultationMemoCard: { backgroundColor: c.paper, borderLeftWidth: 3, borderLeftColor: c.gold, padding: 18, gap: 8 },
  consultationMemoDate: { color: c.gold, fontFamily: "Pretendard-Bold", fontSize: 13, fontWeight: "800" },
  consultationMemoText: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 15, fontWeight: "700", lineHeight: 22 },
  consultationMemoFollow: { color: c.muted, fontFamily: "Pretendard", fontSize: 13, lineHeight: 20 },
  briefHeading: { gap: 7, paddingTop: 8, paddingBottom: 8 },
  briefHero: { backgroundColor: c.ink, padding: 28, gap: 24 },
  briefIcon: { width: 58, height: 58, borderRadius: 12, backgroundColor: c.paper, alignItems: "center", justifyContent: "center" },
  briefSummary: { color: c.paper, fontFamily: "Pretendard-SemiBold", fontSize: 13, lineHeight: 21, fontWeight: "600" },
  briefSuggestion: { flexDirection: "row", alignItems: "center", gap: 14 },
  briefNumber: { width: 36, height: 36, borderRadius: 18, alignItems: "center", justifyContent: "center", backgroundColor: "#F5E5B8" },
  briefNumberText: { color: "#87601D", fontFamily: "Pretendard-Bold", fontSize: 14, fontWeight: "800" },
  aiDisclosure: { borderLeftWidth: 3, borderLeftColor: c.forest, backgroundColor: "#EDF5F1", padding: 18 },
  aiDisclosureText: { color: c.forest, fontFamily: "Pretendard", fontSize: 13, lineHeight: 21 },
  consultationHeading: { gap: 8, paddingTop: 8, paddingBottom: 10 },
  formLabel: { color: c.ink, fontFamily: "Pretendard-Bold", fontSize: 14, fontWeight: "800", marginTop: 4 },
  consultationLargeInput: { minHeight: 128, textAlignVertical: "top" },
  consentBox: { flexDirection: "row", gap: 14, alignItems: "center", justifyContent: "center", padding: 20, borderLeftWidth: 3, borderLeftColor: c.forest, backgroundColor: "#EDF5F1" },
  checkbox: { width: 32, height: 32, borderWidth: 1, borderColor: c.line, borderRadius: 6, backgroundColor: c.paper, alignItems: "center", justifyContent: "center" },
  checkboxChecked: { backgroundColor: c.forest, borderColor: c.forest },
  checkboxTick: { color: c.paper, fontFamily: "Pretendard-Bold", fontWeight: "900" },
  issueHeading: { gap: 8, paddingTop: 10, paddingBottom: 14 },
  issueDetailColumns: { flexDirection: "row", gap: 28, alignItems: "stretch" },
  issueDetailColumnsMobile: { flexDirection: "column" },
  issueVisual: { flex: 3, minHeight: 350, alignItems: "center", justifyContent: "center", gap: 13, padding: 30, backgroundColor: c.ink },
  issueVisualStamp: { width: 154, height: 154 },
  // 검정 카드 위에서만 도장을 분리해 보이게 하는 최소 밝은 원판이다.
  issueStampPlate: { backgroundColor: c.paper, padding: 0, marginBottom: 12 },
  issueInfoColumn: { flex: 1, minWidth: 0 },
  issueInfoCard: { flex: 1, justifyContent: "center" },
  issueVisualStore: { color: c.paper, fontFamily: "Pretendard-Bold", fontSize: 16, fontWeight: "800", textAlign: "center" },
  issueVisualCaption: { color: "#CFC8BC", fontFamily: "Pretendard", fontSize: 10, lineHeight: 15 },
  issueInfoLabel: { fontSize: 10 },
  issueInfoCaption: { fontSize: 10, lineHeight: 14 },
  issueInfoTitle: { fontSize: 15, lineHeight: 21 },
  issueDetailLine: { flexDirection: "row", gap: 12, alignItems: "center", borderTopWidth: 1, borderColor: c.line, paddingTop: 14 },
  issueInfoIcon: { width: 56, height: 56, borderRadius: 28, alignItems: "center", justifyContent: "center", backgroundColor: "#F4E6C4" },
  // 아래 두 버튼의 경계는 위의 이미지 카드/정보 카드 사이 경계와 정확히 맞춘다.
  issueActions: { flexDirection: "row", gap: 28, width: "100%", alignSelf: "stretch" },
  issueAction: { flex: 1, flexBasis: 0, minWidth: 0 },
  issueVisualAction: { flex: 1 },
  issueInfoAction: { flex: 1 },
  issueActionsMobile: { flexDirection: "column", gap: 12 },
  detailActions: { flexDirection: "row", gap: 12, justifyContent: "flex-end" },
  backChevron: { fontFamily: "Pretendard-Bold", fontSize: 28, lineHeight: 30, fontWeight: "900", verticalAlign: "middle" },
  historyOpenCard: { flexDirection: "row", alignItems: "center", gap: 14, padding: 18, borderWidth: 1, borderColor: c.line, borderRadius: 8, backgroundColor: c.paper },
  historyDetailLink: { alignSelf: "flex-start", flexDirection: "row", alignItems: "center", marginTop: 4 },
});
