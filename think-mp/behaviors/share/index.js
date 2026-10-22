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
    _baseShare(params = {}) {
      params.inviterId = getInviterId();
      const query = parseJsonToUrlParam(params);
      const path = `/pages/index/index?${query}`;
      const title = params.title || getRandomTitle();
      const imageUrl = params.imageUrl || "/common/images/share.jpg";
      print("onShareAppMessage", { title, path, imageUrl });
      return { title, path, imageUrl };
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
