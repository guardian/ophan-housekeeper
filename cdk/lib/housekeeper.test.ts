import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { Housekeeper } from "./housekeeper";

describe("The Housekeeper stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new Housekeeper(app, "Housekeeper", { stack: "ophan", stage: "TEST" });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
