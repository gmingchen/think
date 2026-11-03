import { store } from "../../store/index";
import { parseJsonToUrlParam, print } from "../../utils/index";

// 分享文案池
const SHARE_TITLES = [
  "众三思 - 让重要决定被看见",
  "纠结的决定，交给三思 ✨",
  "三思而后行，从众三思开始 🌟",
  "把选择摆出来，听听大家的看法 🙏",
  "重大选择，多一个人多一份思考",
];

// 随机获取分享文案
const getRandomTitle = () =>
  SHARE_TITLES[Math.floor(Math.random() * SHARE_TITLES.length)];

// 获取邀请人ID
const getInviterId = () => store.user?.id || "";

// 分享配置
export const sharePageBehavior = Behavior({
  methods: {
    // 分享配置：path 可选，指定落地页（如决策详情分享直达该决策单），不传则落首页；
    // 落地页已带查询串时用 & 续接，避免第二个 ? 使 inviterId 丢失；
    // title / imageUrl 只作用于分享卡片，不进 query，以免用户标题里的 = & # 破坏参数解析
    _baseShare(options = {}) {
      const { title, imageUrl, path: target = "/pages/index/index", ...rest } = options;
      rest.inviterId = getInviterId();
      const query = parseJsonToUrlParam(rest);
      const path = `${ target }${ target.includes("?") ? "&" : "?" }${ query }`;
      const shareTitle = title || getRandomTitle();
      const shareImageUrl = imageUrl || "/common/images/share.jpg";
      print("onShareAppMessage", { title: shareTitle, path, imageUrl: shareImageUrl });
      return { title: shareTitle, path, imageUrl: shareImageUrl };
    },

    // 分享给好友
    onShareAppMessage(e) {
      const { title } = e.target?.dataset || {};
      return this._baseShare({ title });
    },

    // 分享到朋友圈
    onShareTimeline() {
      return {
        title: getRandomTitle(),
        query: `inviterId=${getInviterId()}`,
      };
    },
  },
});
